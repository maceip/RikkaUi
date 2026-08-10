import { chromium } from "playwright";
import { copyFile, mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const args = new Map();
for (let index = 2; index < process.argv.length; index += 2) {
  args.set(process.argv[index], process.argv[index + 1]);
}

const baseUrl = args.get("--base-url") ?? "http://127.0.0.1:4173";
const siteDir = path.resolve(args.get("--site-dir") ?? "../build/gallery-site");
const registryPath = path.resolve(
  args.get("--registry") ??
    "../feature/docs/src/commonMain/kotlin/zed/rainxch/rikkaui/docs/catalog/ComponentRegistry.kt",
);
const androidSnapshotsDir = path.resolve(
  args.get("--android-snapshots") ?? "../sample/src/test/snapshots/images",
);

const registrySource = await readFile(registryPath, "utf8");
const components = [...registrySource.matchAll(/^ {12}ComponentEntry\(([\s\S]*?)^ {12}\),/gm)]
  .map(([, entry]) => ({
    id: entry.match(/id = "([^"]+)"/)?.[1],
    name: entry.match(/rawName = "([^"]+)"/)?.[1],
    platform: entry.includes("platform = ComponentPlatform.Android") ? "android" : "multiplatform",
  }))
  .filter(({ id, name }) => id && name);

if (components.length === 0) {
  throw new Error(`No components found in ${registryPath}`);
}

const devices = [
  {
    id: "pixel-10-pro",
    label: "Pixel 10 Pro",
    width: 412,
    height: 915,
    frameClass: "phone",
  },
  {
    id: "pixel-10-pro-fold",
    label: "Pixel 10 Pro Fold · unfolded",
    width: 768,
    height: 792,
    frameClass: "fold",
  },
];

const imageDir = path.join(siteDir, "assets", "previews");
await mkdir(imageDir, { recursive: true });

const androidSnapshotNames = {
  "swipeable-row": "swipeableRow",
  glass: "glass",
  call: "call",
};

const browser = await chromium.launch({ headless: true });
const renderers = new Map();
try {
  for (const device of devices) {
    const context = await browser.newContext({
      viewport: { width: device.width, height: device.height },
      deviceScaleFactor: 1,
      colorScheme: "light",
      reducedMotion: "reduce",
    });
    renderers.set(device.id, { context, page: await context.newPage() });
  }

  for (const component of components) {
    for (const device of devices) {
      if (component.platform === "android") {
        const profile = device.frameClass === "phone" ? "GalleryPhoneSnapshotTest" : "GalleryFoldSnapshotTest";
        const method = androidSnapshotNames[component.id];
        if (!method) throw new Error(`No Android snapshot mapping for ${component.id}`);
        await copyFile(
          path.join(androidSnapshotsDir, `dev.rikkaui.sample_${profile}_${method}.png`),
          path.join(imageDir, `${component.id}--${device.id}.png`),
        );
        continue;
      }
      const { page } = renderers.get(device.id);
      const url = `${baseUrl}/#docs/components?componentId=${encodeURIComponent(component.id)}`;
      await page.goto(url, { waitUntil: "networkidle", timeout: 120_000 });
      await page.locator("canvas").waitFor({ state: "visible", timeout: 120_000 });
      await page.waitForTimeout(1_200);
      await page.screenshot({
        path: path.join(imageDir, `${component.id}--${device.id}.png`),
        type: "png",
        animations: "disabled",
      });
    }
    process.stdout.write(`Captured ${component.name}\n`);
  }
} finally {
  await Promise.all([...renderers.values()].map(({ context }) => context.close()));
  await browser.close();
}

const cards = components
  .map(
    (component) => `
      <article class="component-card" data-search="${escapeHtml(`${component.name} ${component.id}`.toLowerCase())}">
        <div class="card-heading">
          <div>
            <p class="eyebrow">${component.platform === "android" ? "Android · Paparazzi" : "Compose component"}</p>
            <h2>${escapeHtml(component.name)}</h2>
          </div>
          <a href="app/#docs/components?componentId=${encodeURIComponent(component.id)}">Open live docs <span aria-hidden="true">↗</span></a>
        </div>
        <div class="devices">
          ${devices
            .map(
              (device) => `
                <figure>
                  <div class="device ${device.frameClass}" aria-label="${escapeHtml(device.label)} frame">
                    <span class="camera" aria-hidden="true"></span>
                    <span class="hinge" aria-hidden="true"></span>
                    <div class="screen">
                      <img
                        src="assets/previews/${component.id}--${device.id}.png"
                        alt="${escapeHtml(component.name)} rendered by Compose at the ${escapeHtml(device.label)} viewport"
                        loading="lazy"
                        width="${device.width}"
                        height="${device.height}"
                      >
                    </div>
                  </div>
                  <figcaption>${escapeHtml(device.label)}</figcaption>
                </figure>`,
            )
            .join("")}
        </div>
      </article>`,
  )
  .join("");

const html = `<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="A generated bitmap gallery of every registered RikkaUI Compose component on Pixel 10 Pro form factors.">
    <title>RikkaUI · Compose component gallery</title>
    <link rel="stylesheet" href="assets/gallery.css">
    <script src="assets/gallery.js" defer></script>
  </head>
  <body>
    <header class="hero">
      <nav>
        <a class="wordmark" href="./" aria-label="RikkaUI gallery home"><span>六花</span> RikkaUI</a>
        <a class="live-link" href="app/#components">Interactive catalog <span aria-hidden="true">↗</span></a>
      </nav>
      <div class="hero-copy">
        <p class="eyebrow">Generated from Kotlin Compose</p>
        <h1>One system.<br>Two real form factors.</h1>
        <p class="lede">Every registered RikkaUI component, captured from Compose/Wasm or Android layoutlib and presented at Pixel 10 Pro and unfolded Pixel 10 Pro Fold form factors.</p>
        <div class="stats" aria-label="Gallery statistics">
          <span><strong>${components.length}</strong> components</span>
          <span><strong>${components.length * devices.length}</strong> Compose captures</span>
          <span><strong>Phosphor</strong> icon family</span>
        </div>
      </div>
    </header>
    <main>
      <div class="toolbar">
        <label for="component-search">Find a component</label>
        <input id="component-search" type="search" placeholder="Search ${components.length} components…" autocomplete="off">
      </div>
      <p id="empty-state" hidden>No components match that search.</p>
      <section class="gallery" aria-label="Component gallery">${cards}</section>
    </main>
    <footer>
      <span>RikkaUI · Compose Multiplatform</span>
      <span>Compose/Wasm + Paparazzi bitmaps generated by the deployment build</span>
    </footer>
  </body>
</html>`;

await mkdir(path.join(siteDir, "assets"), { recursive: true });
await writeFile(path.join(siteDir, "index.html"), html);
await writeFile(path.join(siteDir, ".nojekyll"), "");

function escapeHtml(value) {
  return value.replace(/[&<>"]/g, (character) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
  })[character]);
}
