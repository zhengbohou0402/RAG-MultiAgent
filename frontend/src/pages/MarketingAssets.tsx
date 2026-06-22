import { useState } from "react";
import { App as AntApp, Button, Form, Input, Tag, Typography } from "antd";
import { LinkOutlined, PictureOutlined, RocketOutlined } from "@ant-design/icons";
import { api, resolveBackendUrl } from "../api/client";
import type { MarketingAsset } from "../api/client";
import { useI18n } from "../i18n";

const { Text } = Typography;

export default function MarketingAssets() {
  const { message } = AntApp.useApp();
  const { language } = useI18n();
  const zh = language === "zh";
  const [asset, setAsset] = useState<MarketingAsset | null>(null);
  const [posterUrl, setPosterUrl] = useState("");
  const [landingUrl, setLandingUrl] = useState("");
  const [loading, setLoading] = useState(false);

  async function generate(values: { product_name: string; scenario: string; audience: string }) {
    setLoading(true);
    try {
      const generated = await api.marketing.generate(values);
      setAsset(generated);
      setPosterUrl(await resolveBackendUrl(generated.poster_url));
      setLandingUrl(await resolveBackendUrl(generated.landing_page_url));
      message.success(zh ? "营销素材已生成" : "Marketing assets generated");
    } catch (error) {
      message.error(error instanceof Error ? error.message : (zh ? "生成失败" : "Generation failed"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="admin-shell marketing-shell">
      <div className="admin-header">
        <div>
          <Text type="secondary" className="page-kicker">Marketing Assets</Text>
          <h1>{zh ? "营销素材生成" : "Marketing Asset Generation"}</h1>
          <p>{zh
            ? "本地 deterministic renderer 生成 H5 落地页和 PNG 海报，避免面试演示依赖外部图片模型。"
            : "A local deterministic renderer creates H5 landing pages and PNG posters without relying on an external image model."}</p>
        </div>
        {asset && <Tag color="green">asset: {asset.id}</Tag>}
      </div>
      <div className="marketing-layout">
        <section className="admin-panel">
          <div className="panel-title-row"><Text strong><RocketOutlined /> {zh ? "生成参数" : "Generation Parameters"}</Text></div>
          <Form
            layout="vertical"
            initialValues={{
              product_name: "SmartCloud GPU AI Computing",
              scenario: zh ? "大模型私有化部署推广" : "Private LLM deployment promotion",
              audience: zh ? "企业技术负责人" : "Enterprise technology leaders",
            }}
            onFinish={generate}
          >
            <Form.Item label={zh ? "产品名称" : "Product Name"} name="product_name" rules={[{ required: true, message: zh ? "请输入产品名称" : "Enter product name" }]}>
              <Input placeholder="SmartCloud ECS Standard" />
            </Form.Item>
            <Form.Item label={zh ? "推广场景" : "Campaign Scenario"} name="scenario"><Input placeholder={zh ? "云服务器活动推广" : "Cloud server campaign"} /></Form.Item>
            <Form.Item label={zh ? "目标用户" : "Target Audience"} name="audience"><Input placeholder={zh ? "企业 IT 负责人" : "Enterprise IT leaders"} /></Form.Item>
            <Button type="primary" htmlType="submit" icon={<PictureOutlined />} loading={loading}>{zh ? "生成 H5 与海报" : "Generate H5 and Poster"}</Button>
          </Form>
        </section>
        <section className="admin-panel marketing-preview">
          <div className="panel-title-row">
            <Text strong>{zh ? "素材预览" : "Asset Preview"}</Text>
            {landingUrl && <Button href={landingUrl} target="_blank" icon={<LinkOutlined />}>{zh ? "打开 H5" : "Open H5"}</Button>}
          </div>
          {asset ? (
            <>
              <img src={posterUrl} alt={asset.product_name} />
              <div className="marketing-copy"><Text type="secondary">{zh ? "推广文案" : "Campaign Copy"}</Text><strong>{asset.headline}</strong><p>{asset.campaign_copy}</p></div>
            </>
          ) : (
            <div className="empty-preview">{zh ? "生成后将在这里展示海报和落地页链接" : "The poster and landing page link will appear here after generation."}</div>
          )}
        </section>
      </div>
    </div>
  );
}
