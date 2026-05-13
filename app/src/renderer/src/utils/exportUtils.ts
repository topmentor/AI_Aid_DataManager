type SaveFilter = { name: string; extensions: string[] };

/** headers + rows → CSV (UTF-8 BOM for Excel) */
export async function exportCSV(title: string, headers: string[], rows: string[][]): Promise<void> {
  const escape = (v: string) => `"${String(v ?? "").replace(/"/g, '""')}"`;
  const lines = [
    headers.map(escape).join(","),
    ...rows.map(r => r.map(escape).join(",")),
  ];
  const content = "﻿" + lines.join("\r\n");
  await window.aidclaude.export.saveText(
    `${title}.csv`,
    [{ name: "CSV 파일", extensions: ["csv"] }] satisfies SaveFilter[],
    content
  );
}

/** headers + rows → JSON array of objects */
export async function exportJSON(title: string, headers: string[], rows: string[][]): Promise<void> {
  const data = rows.map(r => Object.fromEntries(headers.map((h, i) => [h, r[i] ?? ""])));
  await window.aidclaude.export.saveText(
    `${title}.json`,
    [{ name: "JSON 파일", extensions: ["json"] }] satisfies SaveFilter[],
    JSON.stringify(data, null, 2)
  );
}

/** canvas element → PNG file */
export async function exportChartPNG(title: string, canvasEl: HTMLCanvasElement): Promise<void> {
  const base64 = canvasEl.toDataURL("image/png").split(",")[1];
  await window.aidclaude.export.saveBinary(
    `${title}.png`,
    [{ name: "PNG 이미지", extensions: ["png"] }] satisfies SaveFilter[],
    base64
  );
}
