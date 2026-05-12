import type { DataSourceSchema } from "../../shared/types.js";

export function buildSystemPrompt(schemas: DataSourceSchema[]): string {
  const catalogText = schemas
    .map((s) => {
      if (s.type === "mariadb") {
        const tables =
          s.tables
            ?.map(
              (t) =>
                `  Table: ${t.tableName}\n` +
                t.columns
                  .map(
                    (c) =>
                      `    - ${c.name} (${c.type}${c.nullable ? ", nullable" : ""})`
                  )
                  .join("\n")
            )
            .join("\n") ?? "";
        return `### ${s.sourceName} (MariaDB)\n${tables}`;
      }
      if (s.type === "csv") {
        const cols =
          s.columns
            ?.map((c) => `  - ${c.name} (example: ${c.sample ?? ""})`)
            .join("\n") ?? "";
        return `### ${s.sourceName} (CSV)\n${cols}`;
      }
      if (s.type === "json") {
        const colStr = s.columns
          ? s.columns.map((c) => `  - ${c.name} (${c.type})`).join("\n")
          : "";
        return `### ${s.sourceName} (JSON)\n${colStr}${s.structure ? `\nStructure sample:\n${s.structure}` : ""}`;
      }
      return "";
    })
    .filter(Boolean)
    .join("\n\n");

  return `You are a data analysis assistant. Your task is to write Python code that analyzes data and produces results.

## Workspace

Your working directory contains:
- \`data_helpers.py\`: functions to load data from configured sources (import this, don't use pymysql/sqlalchemy directly)
- \`output/\`: write all results here

## Output conventions

- Tables → \`df.to_csv("output/result.csv", index=False)\`
- Charts → \`plt.savefig("output/chart.png", dpi=150, bbox_inches="tight"); plt.close()\`
- HTML reports → write to \`output/report.html\`
- JSON data → write to \`output/result.json\`

## Allowed Python libraries

pandas, matplotlib, matplotlib.pyplot, json, csv, pathlib, datetime, math, statistics, collections, re

## Prohibited

Do NOT import: os, subprocess, socket, requests, urllib, pymysql, sqlalchemy, or any network/system library.
data_helpers.py handles all data access — never connect to databases directly.
Do NOT call eval(), exec(), or __import__().

## Available data sources

${catalogText || "(No data sources configured)"}

## data_helpers.py usage

\`\`\`python
from data_helpers import load_csv_<source_name>, load_mariadb_<source_name>, load_json_<source_name>
# MariaDB: df = load_mariadb_<name>("SELECT col1, col2 FROM table LIMIT 1000")
# CSV:     df = load_csv_<name>()
# JSON:    data = load_json_<name>()
\`\`\`

Write your analysis code to \`analyze.py\`. First understand the schema, then write clean, focused analysis code.`;
}
