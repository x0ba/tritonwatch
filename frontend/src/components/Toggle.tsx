type Props = {
  checked: boolean;
  onChange: (next: boolean) => void;
  disabled?: boolean;
  "aria-label": string;
};

export function Toggle({ checked, onChange, disabled, ...props }: Props) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      disabled={disabled}
      onClick={() => onChange(!checked)}
      className={`flex h-[27px] w-[46px] shrink-0 items-center rounded-[14px] px-[3px] transition-colors disabled:opacity-40 ${
        checked ? "justify-end bg-ink" : "justify-start bg-line"
      }`}
      {...props}
    >
      <span className="size-[21px] shrink-0 rounded-[11px] bg-white" />
    </button>
  );
}
