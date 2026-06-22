package com.ftsm.rag.service;

import com.ftsm.rag.config.AppConfig;
import com.ftsm.rag.model.MarketingAsset;
import com.ftsm.rag.model.MarketingRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class MarketingAssetService {

    private final Path root;

    public MarketingAssetService(AppConfig appConfig) {
        this.root = Paths.get(appConfig.getSmartcloud().getMarketingAssetsPath());
    }

    public MarketingAsset generate(MarketingRequest request) {
        try {
            Files.createDirectories(root);
            String id = java.util.UUID.randomUUID().toString();
            String product = blankToDefault(request.getProductName(), "SmartCloud ECS Standard");
            String headline = "Launch-ready cloud resources for growing teams";
            String copy = product + " helps teams deploy faster, scale safer, and keep cloud cost visible.";
            Path assetDir = root.resolve(id);
            Files.createDirectories(assetDir);
            Path html = assetDir.resolve("index.html");
            Path poster = assetDir.resolve("poster.png");
            Files.writeString(html, landingPage(product, headline, copy), StandardCharsets.UTF_8);
            writePoster(poster, product, headline);
            return new MarketingAsset(
                    id,
                    product,
                    headline,
                    "/api/assets/marketing/" + id + "/index.html",
                    "/api/assets/marketing/" + id + "/poster.png",
                    copy
            );
        } catch (Exception error) {
            throw new IllegalStateException("Could not generate marketing asset", error);
        }
    }

    public Resource load(String assetId, String filename) {
        Path path = root.resolve(assetId).resolve(filename).normalize();
        if (!path.startsWith(root) || !Files.exists(path)) {
            return null;
        }
        return new FileSystemResource(path);
    }

    private String landingPage(String product, String headline, String copy) {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>%s Campaign</title>
                  <style>
                    body { margin:0; font-family: Inter, Arial, sans-serif; background:#f3f6fb; color:#102033; }
                    .hero { padding:72px 8vw; background:#092b53; color:white; }
                    .hero h1 { max-width:840px; font-size:44px; line-height:1.1; margin:0 0 18px; }
                    .hero p { max-width:760px; font-size:18px; line-height:1.7; color:#d6e8ff; }
                    .section { padding:40px 8vw; display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:16px; }
                    .card { background:white; border:1px solid #d8e2ef; border-radius:8px; padding:22px; }
                  </style>
                </head>
                <body>
                  <main>
                    <section class="hero"><h1>%s</h1><p>%s</p></section>
                    <section class="section">
                      <div class="card"><strong>Elastic Compute</strong><p>Scale capacity for production workloads.</p></div>
                      <div class="card"><strong>Cost Control</strong><p>Clear billing and renewal recommendations.</p></div>
                      <div class="card"><strong>AI Ready</strong><p>Built for Agentic RAG and LLM deployment paths.</p></div>
                    </section>
                  </main>
                </body>
                </html>
                """.formatted(product, headline, copy);
    }

    private void writePoster(Path path, String product, String headline) throws Exception {
        BufferedImage image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(9, 43, 83));
        g.fillRect(0, 0, 960, 540);
        g.setColor(new Color(22, 119, 255));
        g.fillRoundRect(64, 64, 120, 120, 18, 18);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("SC", 104, 134);
        g.setFont(new Font("Arial", Font.BOLD, 46));
        g.drawString(product, 64, 255);
        g.setFont(new Font("Arial", Font.PLAIN, 28));
        g.setColor(new Color(214, 232, 255));
        g.drawString(headline, 64, 318);
        g.setFont(new Font("Arial", Font.BOLD, 26));
        g.setColor(new Color(99, 230, 190));
        g.drawString("SmartCloud ServiceOps", 64, 450);
        g.dispose();
        ImageIO.write(image, "png", path.toFile());
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
