import { useRef, useState } from "react";
import { streamAssistantReply } from "./api/chat";
import { ChatPane } from "./components/ChatPane";
import { SettingsDialog } from "./components/SettingsDialog";
import { Sidebar } from "./components/Sidebar";
import type { ChatTurn, Conversation, Message } from "./types";

export default function App() {
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [generatingId, setGeneratingId] = useState<string | null>(null);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const abortRef = useRef<AbortController | null>(null);
  const generatingLock = useRef(false);

  const active = conversations.find((conversation) => conversation.id === activeId) ?? null;

  function patchAssistant(conversationId: string, messageId: string, updater: (message: Message) => Message) {
    setConversations((current) =>
      current.map((conversation) => {
        if (conversation.id !== conversationId) return conversation;
        return {
          ...conversation,
          messages: conversation.messages.map((message) =>
            message.id === messageId ? updater(message) : message,
          ),
        };
      }),
    );
  }

  async function handleSend(text: string) {
    const trimmed = text.trim();
    if (!trimmed || generatingLock.current) return;

    generatingLock.current = true;
    const now = new Date().toISOString();
    const conversationId = activeId ?? crypto.randomUUID();
    const existing = conversations.find((conversation) => conversation.id === conversationId);
    const userMessage: Message = {
      id: crypto.randomUUID(),
      role: "user",
      content: trimmed,
      createdAt: now,
    };
    const assistantMessage: Message = {
      id: crypto.randomUUID(),
      role: "assistant",
      content: "",
      createdAt: now,
    };
    const history: ChatTurn[] = [...(existing?.messages ?? []), userMessage]
      .filter((message) => message.content.trim().length > 0)
      .map((message) => ({ role: message.role, content: message.content }));

    setConversations((current) => {
      const found = current.find((conversation) => conversation.id === conversationId);
      if (!found) {
        const created: Conversation = {
          id: conversationId,
          title: titleFrom(trimmed),
          createdAt: now,
          updatedAt: now,
          messages: [userMessage, assistantMessage],
        };
        return [created, ...current];
      }

      const updated: Conversation = {
        ...found,
        updatedAt: now,
        messages: [...found.messages, userMessage, assistantMessage],
      };
      return [updated, ...current.filter((conversation) => conversation.id !== conversationId)];
    });
    setActiveId(conversationId);
    setGeneratingId(conversationId);

    const controller = new AbortController();
    abortRef.current = controller;

    try {
      await streamAssistantReply(history, controller.signal, (token) => {
        patchAssistant(conversationId, assistantMessage.id, (message) => ({
          ...message,
          content: message.content + token,
        }));
      });
    } catch (error) {
      const aborted = error instanceof DOMException && error.name === "AbortError";
      if (aborted) {
        patchAssistant(conversationId, assistantMessage.id, (message) => ({
          ...message,
          stopped: true,
        }));
      } else {
        const detail = error instanceof Error ? error.message : "The reply could not be generated.";
        patchAssistant(conversationId, assistantMessage.id, (message) => ({
          ...message,
          error: detail,
        }));
      }
    } finally {
      abortRef.current = null;
      generatingLock.current = false;
      setGeneratingId((current) => (current === conversationId ? null : current));
    }
  }

  function handleStop() {
    abortRef.current?.abort();
  }

  function handleRename(id: string, title: string) {
    const next = title.replace(/\s+/g, " ").trim();
    if (!next) return;
    setConversations((current) =>
      current.map((conversation) =>
        conversation.id === id ? { ...conversation, title: next.slice(0, 80) } : conversation,
      ),
    );
  }

  function handleDelete(id: string) {
    if (generatingId === id) abortRef.current?.abort();
    setConversations((current) => current.filter((conversation) => conversation.id !== id));
    if (activeId === id) setActiveId(null);
  }

  return (
    <div className="flex h-full min-h-0 bg-white text-neutral-900">
      <Sidebar
        conversations={conversations}
        activeId={activeId}
        onSelect={setActiveId}
        onNewChat={() => setActiveId(null)}
        onRename={handleRename}
        onDelete={handleDelete}
        onOpenSettings={() => setSettingsOpen(true)}
      />
      <ChatPane
        conversation={active}
        streaming={generatingId !== null && generatingId === active?.id}
        busy={generatingId !== null}
        onSend={(text) => void handleSend(text)}
        onStop={handleStop}
      />
      {settingsOpen ? <SettingsDialog onClose={() => setSettingsOpen(false)} /> : null}
    </div>
  );
}

function titleFrom(text: string): string {
  const clean = text.replace(/\s+/g, " ").trim();
  if (clean.length <= 42) return clean;
  return `${clean.slice(0, 41).trimEnd()}…`;
}
