type WelcomeScreenProps = {
  disabled: boolean;
  onSuggest: (text: string) => void;
};

const suggestions = [
  "Explain the difference between a local model and a cloud chatbot",
  "Draft a short checklist for staying organized",
  "Show a Python function that reverses a string",
];

export function WelcomeScreen({ disabled, onSuggest }: WelcomeScreenProps) {
  return (
    <div className="flex h-full items-center justify-center px-6">
      <div className="w-full max-w-xl pb-8">
        <h1 className="text-center text-[28px] font-semibold tracking-tight text-neutral-900">
          How can I help you?
        </h1>
        <p className="mx-auto mt-2 max-w-md text-center text-sm leading-relaxed text-neutral-500">
          Ask a question to start a conversation. Replies stay in this chat so follow-ups keep their context.
        </p>
        <div className="mt-8 grid gap-2">
          {suggestions.map((suggestion) => (
            <button
              key={suggestion}
              type="button"
              disabled={disabled}
              onClick={() => onSuggest(suggestion)}
              className="rounded-2xl border border-neutral-200 px-4 py-3 text-left text-sm text-neutral-700 transition-colors hover:bg-neutral-50 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {suggestion}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
