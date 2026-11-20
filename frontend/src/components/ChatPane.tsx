import type { Conversation } from "../types";
import { Composer } from "./Composer";
import { MessageList } from "./MessageList";
import { WelcomeScreen } from "./WelcomeScreen";

type ChatPaneProps = {
  conversation: Conversation | null;
  streaming: boolean;
  busy: boolean;
  onSend: (text: string) => void;
  onStop: () => void;
};

export function ChatPane({ conversation, streaming, busy, onSend, onStop }: ChatPaneProps) {
  const hasMessages = Boolean(conversation && conversation.messages.length > 0);

  return (
    <section className="flex min-w-0 flex-1 flex-col bg-white">
      <header className="flex h-14 shrink-0 items-center justify-center border-b border-neutral-100 px-4">
        <p className="max-w-xl truncate text-sm font-medium text-neutral-700">
          {conversation?.title ?? "Local Chat"}
        </p>
      </header>
      <div className="min-h-0 flex-1">
        {hasMessages && conversation ? (
          <MessageList conversation={conversation} streaming={streaming} />
        ) : (
          <WelcomeScreen disabled={busy} onSuggest={onSend} />
        )}
      </div>
      <Composer
        conversationId={conversation?.id ?? null}
        busy={busy}
        streaming={streaming}
        onSend={onSend}
        onStop={onStop}
      />
    </section>
  );
}
