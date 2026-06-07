# Karpathy Behavioral Guidelines

These guidelines reduce common LLM coding mistakes. Merge them with project-specific instructions as needed.

Tradeoff: these guidelines bias toward caution over speed. For trivial tasks, use judgment.

## Think Before Coding

Do not assume, hide confusion, or skip tradeoffs.

Before implementing:
- State assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them instead of picking silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop, name what is confusing, and ask.

## Simplicity First

Use the minimum code that solves the problem. Do not add speculative features.

- No features beyond what was asked.
- No abstractions for single-use code.
- No flexibility or configurability that was not requested.
- No error handling for impossible scenarios.
- If 200 lines could be 50, simplify.

Ask: would a senior engineer say this is overcomplicated? If yes, simplify.

## Surgical Changes

Touch only what is necessary. Clean up only your own changes.

When editing existing code:
- Do not improve adjacent code, comments, or formatting unless required.
- Do not refactor things that are not broken.
- Match existing style, even if you would choose differently.
- If unrelated dead code is noticed, mention it instead of deleting it.

When changes create orphans:
- Remove imports, variables, or functions made unused by the current change.
- Do not remove pre-existing dead code unless asked.

Every changed line should trace directly to the user's request.

## Goal-Driven Execution

Define verifiable success criteria and loop until verified.

Transform tasks into concrete checks:
- "Add validation" -> write tests for invalid inputs, then make them pass.
- "Fix the bug" -> write a test that reproduces it, then make it pass.
- "Refactor X" -> ensure tests pass before and after.

For multi-step tasks, state a brief plan:

```text
1. [Step] -> verify: [check]
2. [Step] -> verify: [check]
3. [Step] -> verify: [check]
```

These guidelines are working if diffs have fewer unnecessary changes, fewer rewrites happen due to overcomplication, and clarifying questions come before implementation mistakes.
