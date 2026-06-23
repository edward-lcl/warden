#!/usr/bin/env python3
"""
Warden Issue Classifier
Classifies GitHub issues and takes automated action via gh CLI.
Runs inside GitHub Actions on issue_opened events.
"""

import json
import os
import subprocess
import sys
import anthropic

CATEGORIES = {
    "exploit-report": "A report of a new duplication exploit or bypass of Warden detection",
    "dupe-miss": "A case where Warden failed to detect a known dupe exploit",
    "chunk-bug": "A bug related to chunk loading, data corruption, or world state",
    "feature-request": "A request for new functionality or configuration options",
    "duplicate": "An issue that duplicates an existing open issue",
    "invalid": "Noise, spam, test issues, or reports that lack actionable information",
}

AUTO_CLOSE_CATEGORIES = {"duplicate", "invalid"}

COMMENT_TEMPLATES = {
    "exploit-report": """Thanks for the exploit report. I've classified this as an **exploit report** and queued it for analysis.

A fix-proposal agent will review this and open a PR if a patch is viable. You'll see activity on this issue within 24-48 hours.

To help the patch land faster, please include:
- Server version (Paper/Spigot + Minecraft version)
- Steps to reproduce
- Any screenshots or logs from `plugins/Warden/warden.log`""",

    "dupe-miss": """Thanks for the detection miss report. I've classified this as a **dupe detection miss** and queued it for a classifier improvement patch.

Please include:
- The dupe method used
- Your Warden config (especially `thresholds` section)
- Whether this is reproducible""",

    "chunk-bug": """Thanks for the bug report. I've classified this as a **chunk-level bug** and queued it for investigation.

Please include:
- Server version and world type
- Steps to reproduce
- Any relevant console errors""",

    "feature-request": """Thanks for the feature request. I've classified this as a **feature request** and added it to the backlog.

We'll evaluate this against the project roadmap. Feel free to add any additional context about your use case.""",

    "duplicate": """This issue appears to duplicate an existing report. I've closed it to keep the tracker clean.

Please add your information to the original issue — every detail helps.""",

    "invalid": """This issue doesn't contain enough information to act on. I've closed it.

If you have a real bug report or exploit, please open a new issue with:
- Clear reproduction steps
- Server version
- Relevant logs""",
}


def classify_issue(title: str, body: str) -> dict:
    client = anthropic.Anthropic(api_key=os.environ["ANTHROPIC_API_KEY"])

    categories_desc = "\n".join(f"- **{k}**: {v}" for k, v in CATEGORIES.items())

    message = client.messages.create(
        model="claude-haiku-4-5-20251001",
        max_tokens=256,
        messages=[
            {
                "role": "user",
                "content": f"""You are classifying GitHub issues for Warden, a Minecraft server plugin that detects item duplication exploits and chunk bugs.

Categories:
{categories_desc}

Issue title: {title}
Issue body:
{body}

Respond with JSON only:
{{"category": "<category>", "confidence": <0.0-1.0>, "reasoning": "<one sentence>"}}""",
            }
        ],
    )

    return json.loads(message.content[0].text)


def run(cmd: str) -> str:
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"WARN: {cmd} failed: {result.stderr}", file=sys.stderr)
    return result.stdout.strip()


def main():
    issue_number = os.environ["ISSUE_NUMBER"]
    issue_title = os.environ["ISSUE_TITLE"]
    issue_body = os.environ.get("ISSUE_BODY", "")
    repo = os.environ["GITHUB_REPOSITORY"]

    print(f"Classifying issue #{issue_number}: {issue_title}")

    result = classify_issue(issue_title, issue_body)
    category = result["category"]
    confidence = result["confidence"]
    reasoning = result["reasoning"]

    print(f"Category: {category} (confidence: {confidence:.2f})")
    print(f"Reasoning: {reasoning}")

    # Apply label
    run(f'gh issue edit {issue_number} --add-label "{category}" --repo {repo}')

    if category not in AUTO_CLOSE_CATEGORIES:
        run(f'gh issue edit {issue_number} --add-label "awaiting-fix" --repo {repo}')

    # Post comment
    comment = COMMENT_TEMPLATES.get(category, "Issue classified and queued.")
    comment_with_meta = f"{comment}\n\n---\n*🤖 Warden classifier — category: `{category}`, confidence: `{confidence:.0%}`*"

    # Write comment to temp file to avoid shell escaping issues
    with open("/tmp/warden_comment.md", "w") as f:
        f.write(comment_with_meta)

    run(f"gh issue comment {issue_number} --body-file /tmp/warden_comment.md --repo {repo}")

    # Auto-close if noise/duplicate
    if category in AUTO_CLOSE_CATEGORIES:
        run(f"gh issue close {issue_number} --repo {repo}")
        print(f"Auto-closed issue #{issue_number} as {category}")

    print("Done.")


if __name__ == "__main__":
    main()
