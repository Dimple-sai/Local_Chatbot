import { useEffect, useRef, useState } from "react";
import { SendIcon, StopIcon } from "./Icons";

type ComposerProps = {
  conversationId: string | null;
  busy: boolean;
  streaming: boolean;
  onSend: (text: string) => void;
  onStop: () => void;
};

export function Composer({ conversationId, busy, streaming, onSend, onStop }: ComposerProps) {
  const [text, setText] = useState("");
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    setText("");
    const textarea = textareaRef.current;
    if (!textarea) return;
    textarea.style.height = "auto";
    textarea.focus();
  }, [conversationId]);

  function resize(element: HTMLTextAreaElement) {
    element.style.height = "auto";
    element.style.height = `${Math.min(element.scrollHeight, 160)}px`;
  }

  function submit() {
    const trimmed = text.trim();
    if (!trimmed || busy) return;
    onSend(trimmed);
    setText("");
    const textarea = textareaRef.current;
    if (textarea) textarea.style.height = "auto";
  }

  return (
    <div className="px-4 pb-4 pt-2">
      <form
        className="mx-auto max-w-3xl"
        onSubmit={(event) => {
          event.preventDefault();
          submit();
        }}
      >
        <div className="flex items-end gap-2 rounded-[28px] border border-neutral-200 bg-white py-2 pl-4 pr-2 shadow-[0_8px_24px_rgba(0,0,0,0.04)]">
          <label htmlFor="chat-input" className="sr-only">
            Message
          </label>
          <textarea
            id="chat-input"
            ref={textareaRef}
            rows={1}
            value={text}
            placeholder="Message Local Chat"
            enterKeyHint="send"
            autoComplete="off"
            className="max-h-40 min-h-6 flex-1 resize-none bg-transparent py-2 text-[15px] leading-6 text-neutral-900 outline-none placeholder:text-neutral-400"
            onChange={(event) => {
              setText(event.target.value);
              resize(event.target);
            }}
            onKeyDown={(event) => {
              if (event.key === "Enter" && !event.shiftKey) {
                event.preventDefault();
                submit();
              }
            }}
          />
          {busy ? (
            <button
              type="button"
              onClick={onStop}
              aria-label="Stop generating"
              className="mb-0.5 grid h-9 w-9 shrink-0 place-items-center rounded-full bg-neutral-900 text-white hover:bg-neutral-700"
            >
              <StopIcon />
            </button>
          ) : (
            <button
              type="submit"
              disabled={!text.trim()}
              aria-label="Send message"
              className="mb-0.5 grid h-9 w-9 shrink-0 place-items-center rounded-full bg-neutral-900 text-white hover:bg-neutral-700 disabled:bg-neutral-200 disabled:text-neutral-400"
            >
              <SendIcon />
            </button>
          )}
        </div>
        <p className="mt-2 text-center text-xs text-neutral-400">
          {busy && !streaming
            ? "A reply is still generating."
            : "Preview replies until a local model is connected. Nothing leaves this computer."}
        </p>
      </form>
    </div>
  );
}
