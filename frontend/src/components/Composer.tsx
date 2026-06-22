import { useState } from "react";
import { Button, Input } from "antd";
import { SendOutlined } from "@ant-design/icons";
import { useI18n } from "../i18n";

const { TextArea } = Input;

interface Props {
  onSend: (text: string) => void;
  disabled: boolean;
}

export default function Composer({ onSend, disabled }: Props) {
  const [text, setText] = useState("");
  const { t } = useI18n();
  const suggestions = [
    { title: t("suggest.ecs"), prompt: t("suggest.ecsPrompt") },
    { title: t("suggest.gpu"), prompt: t("suggest.gpuPrompt") },
    { title: t("suggest.billing"), prompt: t("suggest.billingPrompt") },
    { title: t("suggest.invoice"), prompt: t("suggest.invoicePrompt") },
    { title: t("suggest.icp"), prompt: t("suggest.icpPrompt") },
    { title: t("suggest.poster"), prompt: t("suggest.posterPrompt") },
    { title: t("suggest.research"), prompt: t("suggest.researchPrompt") },
    { title: t("suggest.refresh"), prompt: t("suggest.refreshPrompt") },
  ];

  const handleSend = () => {
    const trimmed = text.trim();
    if (!trimmed || disabled) return;
    onSend(trimmed);
    setText("");
  };

  return (
    <div className="composer-wrap">
      <div className="suggestions-row">
        {suggestions.map((suggestion) => (
          <Button
            key={suggestion.title}
            size="small"
            className="suggestion-chip"
            disabled={disabled}
            onClick={() => onSend(suggestion.prompt)}
          >
            {suggestion.title}
          </Button>
        ))}
      </div>
      <div className="composer">
        <TextArea
          value={text}
          onChange={(event) => setText(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter" && !event.shiftKey) {
              event.preventDefault();
              handleSend();
            }
          }}
          placeholder={t("composer.placeholder")}
          autoSize={{ minRows: 1, maxRows: 6 }}
          maxLength={1500}
          disabled={disabled}
        />
        <Button
          type="primary"
          icon={<SendOutlined />}
          onClick={handleSend}
          disabled={disabled || !text.trim()}
        />
      </div>
      <p className="disclaimer">{t("composer.disclaimer")}</p>
    </div>
  );
}
