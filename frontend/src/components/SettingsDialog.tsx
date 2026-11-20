import { useEffect, useRef } from "react";
import { CloseIcon } from "./Icons";

type SettingsDialogProps = {
  onClose: () => void;
};

const details = [
  ["Runtime", "This computer"],
  ["Model", "Not configured"],
  ["Local API", "http://127.0.0.1:8000"],
  ["History", "This window only, for now"],
];

export function SettingsDialog({ onClose }: SettingsDialogProps) {
  const closeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    closeRef.current?.focus();
  }, []);

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }

    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  return (
    <div
      className="fixed inset-0 z-20 flex items-center justify-center bg-neutral-900/30 px-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="settings-title"
        className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 id="settings-title" className="text-base font-semibold text-neutral-900">
              Settings
            </h2>
            <p className="mt-1 text-sm text-neutral-500">Local chat configuration</p>
          </div>
          <button
            ref={closeRef}
            type="button"
            onClick={onClose}
            aria-label="Close settings"
            className="rounded-lg p-1.5 text-neutral-500 hover:bg-neutral-100 hover:text-neutral-800"
          >
            <CloseIcon />
          </button>
        </div>

        <dl className="mt-5 divide-y divide-neutral-100 border-y border-neutral-100 text-sm">
          {details.map(([label, value]) => (
            <div key={label} className="flex items-center justify-between gap-4 py-3">
              <dt className="text-neutral-500">{label}</dt>
              <dd className="text-right text-neutral-800">{value}</dd>
            </div>
          ))}
        </dl>

        <p className="mt-4 text-sm leading-relaxed text-neutral-500">
          The model file will be selected here once the local runtime is connected. Replies are
          generated on this computer, with no cloud model and no account.
        </p>
      </div>
    </div>
  );
}
