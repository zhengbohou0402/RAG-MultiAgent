import { useState } from "react";
import { Modal, Input, Button, Typography, Radio, Space, message } from "antd";
import { EyeOutlined, EyeInvisibleOutlined } from "@ant-design/icons";
import { Link } from "react-router-dom";
import { useSettings } from "../hooks/useSettings";
import setupIcon from "../assets/api-key-setup-icon.png";
import { useI18n } from "../i18n";

const { Text, Paragraph } = Typography;

const INTL_BASE_DEFAULT = "https://dashscope-intl.aliyuncs.com/api/v1";

interface Props {
  open: boolean;
  onSuccess: () => void;
}

export default function ApiKeySetupModal({ open, onSuccess }: Props) {
  const { saving, save } = useSettings();
  const [apiKey, setApiKey] = useState("");
  const [showKey, setShowKey] = useState(false);
  const [region, setRegion] = useState<"china" | "intl">("china");
  const [intlBase, setIntlBase] = useState(INTL_BASE_DEFAULT);
  const { t } = useI18n();

  const handleSave = async () => {
    const key = apiKey.trim();
    if (!key) {
      message.error(t("apiKey.required"));
      return;
    }
    const baseUrl = region === "intl" ? (intlBase.trim() || INTL_BASE_DEFAULT) : "";
    const ok = await save({
      dashscope_api_key: key,
      dashscope_base_url: baseUrl,
      chat_model_name: "qwen-turbo",
    });
    if (ok) {
      message.success(t("apiKey.saved"));
      setApiKey("");
      onSuccess();
    } else {
      message.error(t("apiKey.failed"));
    }
  };

  return (
    <Modal
      title={
        <div style={{ textAlign: "center", padding: "4px 0 0" }}>
          <img
            src={setupIcon}
            alt="SmartCloud"
            width={72}
            height={72}
            style={{
              borderRadius: 16,
              display: "block",
              margin: "0 auto 14px",
              boxShadow: "0 8px 24px rgba(15, 118, 110, 0.22)",
            }}
          />
          <span style={{ fontSize: 17, fontWeight: 600 }}>{t("apiKey.title")}</span>
        </div>
      }
      open={open}
      closable={false}
      keyboard={false}
      footer={null}
      width={480}
      centered
      destroyOnHidden
      mask={{ closable: false }}
    >
      <Paragraph type="secondary" style={{ marginBottom: 16, textAlign: "center" }}>
        {t("apiKey.desc")}
      </Paragraph>
      <Space orientation="vertical" size="middle" style={{ width: "100%" }}>
        <div>
          <Text strong style={{ display: "block", marginBottom: 8 }}>
            {t("apiKey.key")}
          </Text>
          <Input
            type={showKey ? "text" : "password"}
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
            placeholder="sk-xxxxxxxx"
            autoComplete="off"
            suffix={
              <Button
                type="text"
                size="small"
                icon={showKey ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                onClick={() => setShowKey(!showKey)}
              />
            }
          />
        </div>
        <div>
          <Text strong style={{ display: "block", marginBottom: 8 }}>
            {t("apiKey.region")}
          </Text>
          <Radio.Group
            value={region}
            onChange={(e) => setRegion(e.target.value)}
            buttonStyle="solid"
          >
            <Radio.Button value="china">{t("apiKey.china")}</Radio.Button>
            <Radio.Button value="intl">{t("apiKey.intl")}</Radio.Button>
          </Radio.Group>
        </div>
        {region === "intl" && (
          <div>
            <Text strong style={{ display: "block", marginBottom: 8 }}>
              {t("apiKey.baseUrl")}
            </Text>
            <Input
              value={intlBase}
              onChange={(e) => setIntlBase(e.target.value)}
              placeholder={INTL_BASE_DEFAULT}
            />
          </div>
        )}
        <Button type="primary" block onClick={handleSave} loading={saving}>
          {t("apiKey.save")}
        </Button>
        <Text type="secondary" style={{ fontSize: 12, display: "block", textAlign: "center" }}>
          {t("apiKey.fullSettings").replace(t("apiKey.openSettings"), "")}
          <Link to="/settings">{t("apiKey.openSettings")}</Link>
        </Text>
      </Space>
    </Modal>
  );
}
