import { useState } from "react";
import ReactMarkdown, { type Components } from "react-markdown";
import remarkGfm from "remark-gfm";

type MarkdownContentProps = {
  content: string;
};

const components: Components = {
  a({ href, children }) {
    return (
      <a href={href} target="_blank" rel="noreferrer" className="underline underline-offset-2">
        {children}
      </a>
    );
  },
  pre({ children }) {
    return <>{children}</>;
  },
  code({ className, children }) {
    const text = String(children).replace(/\n$/, "");
    const language = /language-([\w-]+)/.exec(className ?? "")?.[1];
    const block = Boolean(language) || text.includes("\n");

    if (!block) {
      return <code className={className}>{children}</code>;
    }

    return <CodeBlock language={language ?? "text"} code={text} />;
  },
};

export function MarkdownContent({ content }: MarkdownContentProps) {
  return (
    <div className="prose prose-neutral max-w-none text-[15px] leading-relaxed prose-p:my-2 prose-headings:mb-2 prose-headings:mt-4 prose-headings:text-[15px] prose-headings:font-semibold prose-ul:my-2 prose-ol:my-2 prose-li:my-0.5 prose-table:my-3 prose-code:rounded prose-code:bg-neutral-100 prose-code:px-1 prose-code:py-0.5 prose-code:font-normal prose-code:text-[0.9em] prose-code:before:content-none prose-code:after:content-none">
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
        {content}
      </ReactMarkdown>
    </div>
  );
}

function CodeBlock({ language, code }: { language: string; code: string }) {
  const [copied, setCopied] = useState(false);

  async function copy() {
    try {
      await navigator.clipboard.writeText(code);
      setCopied(true);
      window.setTimeout(() => setCopied(false), 1500);
    } catch {
      setCopied(false);
    }
  }

  return (
    <div className="not-prose my-3 overflow-hidden rounded-xl border border-neutral-200 bg-neutral-50">
      <div className="flex items-center justify-between border-b border-neutral-200 px-3 py-1.5 text-xs text-neutral-500">
        <span>{language}</span>
        <button type="button" onClick={() => void copy()} className="rounded px-1.5 py-0.5 hover:bg-neutral-200 hover:text-neutral-800">
          {copied ? "Copied" : "Copy"}
        </button>
      </div>
      <pre className="overflow-x-auto p-3 text-[13px] leading-relaxed break-words whitespace-pre-wrap text-neutral-800">
        <code>{code}</code>
      </pre>
    </div>
  );
}
