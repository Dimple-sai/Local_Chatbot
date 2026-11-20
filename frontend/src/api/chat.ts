import type { ChatTurn } from "../types";

/**
 * Phase 1 chat client.
 * Streams a local simulated reply so the interface can be used before FastAPI
 * and llama.cpp are connected. Later phases replace this function with a
 * client for the local API at http://127.0.0.1:8000. It does not call a network service.
 */
export async function streamAssistantReply(
  history: ChatTurn[],
  signal: AbortSignal,
  onToken: (token: string) => void,
): Promise<void> {
  const reply = composeMockReply(history);
  await wait(450, signal);
  const tokens = reply.match(/\s+|\S+/g) ?? [reply];

  for (const token of tokens) {
    if (signal.aborted) {
      throw new DOMException("The reply was stopped.", "AbortError");
    }
    onToken(token);
    await wait(16, signal);
  }
}

function composeMockReply(history: ChatTurn[]): string {
  const userTurns = history.filter((turn) => turn.role === "user" && turn.content.trim());
  const latest = userTurns[userTurns.length - 1]?.content.trim() ?? "";
  const previous = userTurns.length > 1 ? userTurns[userTurns.length - 2].content.trim() : "";
  const context = previous
    ? `Continuing from earlier in this chat, where you mentioned “${clip(previous, 120)}”.\n\n`
    : "";
  const lower = latest.toLowerCase();

  if (/\b(code|function|python|javascript|sql)\b/.test(lower)) {
    return `${context}Here is a small Python function for that request.\n\n${pythonExample()}\n\nIt walks the string from the end back to the start. Replace \`"local chat"\` with the text you want to reverse.`;
  }

  if (/\b(difference|cloud|local model|offline)\b/.test(lower)) {
    return `${context}A local model and a cloud chatbot answer in a similar chat, but they run in different places.\n\n| | Local model | Cloud chatbot |\n| --- | --- | --- |\n| Where it runs | On this computer | On a provider's servers |\n| Network | Not required after setup | Required for every reply |\n| History | Stored on this computer | Stored with the provider |\n\n**What that means here**\n- Questions and replies stay in this application.\n- A follow-up can use the earlier messages in the same conversation.\n- The model file is chosen locally, so it can be swapped without changing the chat screen.`;
  }

  if (/\b(checklist|plan|organiz|steps)\b/.test(lower)) {
    return `${context}A short checklist you can reuse:\n\n1. Write the outcome in one sentence.\n2. List the pieces that have to exist before that outcome is real.\n3. Do the next concrete piece, then check it.\n4. Leave a note about what is still open.\n\nFor “${clip(latest, 140)}”, step 2 is the useful one: name the pieces before adding more scope.`;
  }

  if (/^(hi|hello|hey)\b/i.test(latest)) {
    return `${context}Hello. Ask a question in the box below and the reply will stay in this conversation.`;
  }

  return `${context}Here is a direct way to approach “${clip(latest, 180)}”.\n\nTreat the request as something this chat should answer in place: restate the goal, keep the constraint that matters, and ask for one kind of output.\n\n**A practical next step**\n- Say what finished looks like.\n- Mention any limit that matters, such as length, audience, or format.\n- Ask for a summary, a list, or a draft.`;
}

function pythonExample(): string {
  return [
    "```python",
    "def reverse_text(value: str) -> str:",
    '    """Return the characters of value in reverse order."""',
    "    return value[::-1]",
    "",
    'if __name__ == "__main__":',
    '    print(reverse_text("local chat"))',
    "```",
  ].join("\n");
}

function clip(text: string, max: number): string {
  const clean = text.replace(/\s+/g, " ").replace(/[`*_[\]]/g, "").trim();
  if (clean.length <= max) return clean;
  return `${clean.slice(0, max - 1).trimEnd()}…`;
}

function wait(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal.aborted) {
      reject(new DOMException("The reply was stopped.", "AbortError"));
      return;
    }

    const timer = window.setTimeout(() => {
      signal.removeEventListener("abort", onAbort);
      resolve();
    }, ms);

    const onAbort = () => {
      window.clearTimeout(timer);
      reject(new DOMException("The reply was stopped.", "AbortError"));
    };

    signal.addEventListener("abort", onAbort, { once: true });
  });
}
