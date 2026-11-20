import { useEffect, useRef, useState } from "react";
import type { Conversation, Message } from "../types";
import { MarkdownContent } from "./MarkdownContent";

type MessageListProps = {
  conversation: Conversation;
  streaming: boolean;
};

export function MessageList({ conversation, streaming }: MessageListProps) {
  const scrollerRef = useRef<HTMLDivElement>(null);
  const stickRef = useRef(true);

  useEffect(() => {
    stickRef.current = true;
  }, [conversation.id]);

  useEffect(() => {
    const scroller = scrollerRef.current;
    if (!scroller || !stickRef.current) return;
    scroller.scrollTop = scroller.scrollHeight;
  }, [conversation.messages, streaming]);

  function onScroll() {
    const scroller = scrollerRef.current;
    if (!scroller) return;
    stickRef.current = scroller.scrollHeight - scroller.scrollTop - scroller.clientHeight < 80;
  }

  return (
    <div ref={scrollerRef} onScroll={onScroll} className="h-full overflow-y-auto">
      <div className="mx-auto flex max-w-3xl flex-col gap-6 px-4 py-8">
        {conversation.messages.map((message, index) => (
          <MessageRow
            key={message.id}
            message={message}
            streaming={streaming && index === conversation.messages.length - 1}
          />
        ))}
        {streaming ? <span className="sr-only">Generating reply</span> : null}
      </div>
    </div>
  );
}

function MessageRow({ message, streaming }: { message: Message; streaming: boolean }) {
  if (message.role === "user") {
    return (
      <div className="flex justify-end">
        <div className="max-w-[min(85%,40rem)] whitespace-pre-wrap rounded-3xl bg-neutral-100 px-4 py-2.5 text-[15px] leading-relaxed text-neutral-900">
          {message.content}
        </div>
      </div>
    );
  }

  const showIndicator = streaming && message.content.length === 0 && !message.error;
  const showStopped = message.stopped && message.content.length === 0 && !message.error;

  return (
    <div>
      {showIndicator ? <TypingIndicator /> : null}
      {message.error ? <p className="text-sm text-red-700">{message.error}</p> : null}
      {showStopped ? <p className="text-sm text-neutral-400">Generation stopped.</p> : null}
      {message.content ? (
        <>
          <MarkdownContent content={message.content} />
          <div className="mt-1 flex items-center gap-2">
            <CopyButton text={message.content} />
            {message.stopped ? <span className="text-xs text-neutral-400">Stopped</span> : null}
          </div>
        </>
      ) : null}
    </div>
  );
}

function TypingIndicator() {
  return (
    <div className="flex items-center gap-1 py-2" role="status" aria-label="Generating">
      <span className="typing-dot" />
      <span className="typing-dot" />
      <span className="typing-dot" />
    </div>
  );
}

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1500);
    } catch {
      setCopied(false);
    }
  }

  return (
    <button
      type="button"
      onClick={() => void copy()}
      className="rounded-md px-2 py-1 text-xs text-neutral-500 hover:bg-neutral-100 hover:text-neutral-800"
    >
      {copied ? "Copied" : "Copy"}
    </button>
  );
}
