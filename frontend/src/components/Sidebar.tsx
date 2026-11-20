import { useRef, useState } from "react";
import type { Conversation } from "../types";
import { LogoMark, PencilIcon, PlusIcon, SearchIcon, SettingsIcon, TrashIcon } from "./Icons";

type SidebarProps = {
  conversations: Conversation[];
  activeId: string | null;
  onSelect: (id: string) => void;
  onNewChat: () => void;
  onRename: (id: string, title: string) => void;
  onDelete: (id: string) => void;
  onOpenSettings: () => void;
};

export function Sidebar({
  conversations,
  activeId,
  onSelect,
  onNewChat,
  onRename,
  onDelete,
  onOpenSettings,
}: SidebarProps) {
  const [query, setQuery] = useState("");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [draftTitle, setDraftTitle] = useState("");
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
  const skipCommit = useRef(false);

  const needle = query.trim().toLowerCase();
  const filtered = conversations.filter((conversation) =>
    conversation.title.toLowerCase().includes(needle),
  );
  const groups = groupByDay(filtered);

  function startRename(conversation: Conversation) {
    setConfirmDeleteId(null);
    setEditingId(conversation.id);
    setDraftTitle(conversation.title);
  }

  function commitRename(id: string) {
    if (skipCommit.current) {
      skipCommit.current = false;
      return;
    }
    onRename(id, draftTitle);
    setEditingId(null);
  }

  function cancelRename() {
    skipCommit.current = true;
    setEditingId(null);
  }

  return (
    <aside className="flex h-full w-[272px] shrink-0 flex-col border-r border-neutral-200 bg-neutral-50">
      <div className="flex flex-col gap-3 p-3">
        <div className="flex items-center gap-2.5 px-1 py-1">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-neutral-900 text-white">
            <LogoMark />
          </span>
          <div>
            <p className="text-sm font-semibold leading-none text-neutral-900">Local Chat</p>
            <p className="mt-1 text-[11px] text-neutral-500">On this computer</p>
          </div>
        </div>

        <button
          type="button"
          onClick={onNewChat}
          className="flex w-full items-center gap-2 rounded-xl border border-neutral-200 bg-white px-3 py-2.5 text-sm font-medium text-neutral-800 shadow-sm hover:bg-neutral-50"
        >
          <PlusIcon />
          New chat
        </button>

        <div className="relative">
          <span className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-neutral-400">
            <SearchIcon />
          </span>
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Search chats"
            aria-label="Search chats"
            className="w-full rounded-xl border border-neutral-200 bg-white py-2 pr-3 pl-9 text-sm outline-none placeholder:text-neutral-400"
          />
        </div>
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto px-2 pb-3" aria-label="Previous conversations">
        {conversations.length === 0 ? (
          <p className="px-3 py-6 text-sm text-neutral-400">No conversations yet</p>
        ) : filtered.length === 0 ? (
          <p className="px-3 py-6 text-sm text-neutral-400">No matches</p>
        ) : (
          groups.map((group) => (
            <section key={group.label} className="mb-3">
              <h2 className="px-2 py-1.5 text-[11px] font-medium tracking-wide text-neutral-400 uppercase">
                {group.label}
              </h2>
              <div className="flex flex-col gap-0.5">
                {group.items.map((conversation) => {
                  const active = conversation.id === activeId;
                  const editing = editingId === conversation.id;
                  const confirming = confirmDeleteId === conversation.id;

                  if (confirming) {
                    return (
                      <div
                        key={conversation.id}
                        className="flex items-center justify-between gap-2 rounded-lg bg-white px-2 py-2 shadow-sm"
                      >
                        <span className="text-xs text-neutral-600">Delete this chat?</span>
                        <span className="flex shrink-0 gap-1">
                          <button
                            type="button"
                            onClick={() => setConfirmDeleteId(null)}
                            className="rounded-md px-2 py-1 text-xs text-neutral-500 hover:bg-neutral-100"
                          >
                            Cancel
                          </button>
                          <button
                            type="button"
                            onClick={() => {
                              onDelete(conversation.id);
                              setConfirmDeleteId(null);
                              if (editingId === conversation.id) setEditingId(null);
                            }}
                            className="rounded-md px-2 py-1 text-xs font-medium text-red-700 hover:bg-red-50"
                          >
                            Delete
                          </button>
                        </span>
                      </div>
                    );
                  }

                  return (
                    <div
                      key={conversation.id}
                      className={`group flex items-center gap-1 rounded-lg px-2 py-2 ${
                        active ? "bg-white shadow-sm" : "hover:bg-white/80"
                      }`}
                    >
                      {editing ? (
                        <form
                          className="min-w-0 flex-1"
                          onSubmit={(event) => {
                            event.preventDefault();
                            commitRename(conversation.id);
                          }}
                        >
                          <input
                            autoFocus
                            value={draftTitle}
                            aria-label="Conversation title"
                            onChange={(event) => setDraftTitle(event.target.value)}
                            onBlur={() => commitRename(conversation.id)}
                            onKeyDown={(event) => {
                              if (event.key === "Escape") {
                                event.preventDefault();
                                cancelRename();
                              }
                            }}
                            className="w-full rounded-md border border-neutral-300 bg-white px-2 py-1 text-sm outline-none"
                          />
                        </form>
                      ) : (
                        <button
                          type="button"
                          onClick={() => onSelect(conversation.id)}
                          title={conversation.title}
                          className="min-w-0 flex-1 truncate text-left text-sm text-neutral-800"
                          aria-current={active ? "page" : undefined}
                        >
                          {conversation.title}
                        </button>
                      )}
                      <span
                        className={`flex shrink-0 items-center ${
                          active || editing ? "opacity-100" : "opacity-0 group-hover:opacity-100 group-focus-within:opacity-100"
                        }`}
                      >
                        <button
                          type="button"
                          aria-label={`Rename ${conversation.title}`}
                          onMouseDown={(event) => event.preventDefault()}
                          onClick={() => startRename(conversation)}
                          className="rounded-md p-1.5 text-neutral-400 hover:bg-neutral-100 hover:text-neutral-700"
                        >
                          <PencilIcon />
                        </button>
                        <button
                          type="button"
                          aria-label={`Delete ${conversation.title}`}
                          onClick={() => {
                            setEditingId(null);
                            setConfirmDeleteId(conversation.id);
                          }}
                          className="rounded-md p-1.5 text-neutral-400 hover:bg-neutral-100 hover:text-neutral-700"
                        >
                          <TrashIcon />
                        </button>
                      </span>
                    </div>
                  );
                })}
              </div>
            </section>
          ))
        )}
      </div>

      <div className="border-t border-neutral-200 p-3">
        <button
          type="button"
          onClick={onOpenSettings}
          className="flex w-full items-center gap-2 rounded-xl px-2 py-2 text-sm text-neutral-700 hover:bg-white"
        >
          <SettingsIcon />
          Settings
        </button>
        <p className="px-2 pt-1 text-[11px] text-neutral-400">Runs locally. No account.</p>
      </div>
    </aside>
  );
}

function groupByDay(conversations: Conversation[]): { label: string; items: Conversation[] }[] {
  const groups: { label: string; items: Conversation[] }[] = [];

  for (const conversation of conversations) {
    const label = dayLabel(conversation.updatedAt);
    const existing = groups.find((group) => group.label === label);
    if (existing) existing.items.push(conversation);
    else groups.push({ label, items: [conversation] });
  }

  return groups;
}

function dayLabel(iso: string): string {
  const date = new Date(iso);
  const today = startOfDay(new Date());
  const thatDay = startOfDay(date);
  const diff = Math.round((today - thatDay) / 86_400_000);

  if (diff === 0) return "Today";
  if (diff === 1) return "Yesterday";
  return date.toLocaleDateString(undefined, { month: "short", day: "numeric" });
}

function startOfDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}
