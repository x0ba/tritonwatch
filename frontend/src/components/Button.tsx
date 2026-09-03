import type { ButtonHTMLAttributes, ReactNode } from "react";

type Variant = "primary" | "outline" | "ghost";

const variants: Record<Variant, string> = {
  primary: "bg-ink text-white hover:bg-ink/90 disabled:bg-ink/40 disabled:cursor-not-allowed",
  outline: "border border-ink bg-white text-ink hover:bg-ink/[0.03] disabled:opacity-40",
  ghost: "bg-transparent text-muted hover:text-ink",
};

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  children: ReactNode;
};

export function Button({
  variant = "primary",
  className = "",
  children,
  type = "button",
  ...props
}: Props) {
  return (
    <button
      type={type}
      className={`inline-flex items-center justify-center rounded-sm px-5 py-3.5 text-[15px] font-medium leading-[18px] transition-colors ${variants[variant]} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
}
