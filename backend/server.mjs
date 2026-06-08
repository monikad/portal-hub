import { createServer } from "node:http";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const statePath = join(__dirname, "..", "data", "portal-state.json");
const personalOsDir = process.env.PERSONAL_OS_DIR || join(__dirname, "..", "..", "personal-os");
const port = Number(process.env.PORT || 8787);
const localModelUrl = process.env.LOCAL_MODEL_URL || "";
const localModelName = process.env.LOCAL_MODEL_NAME || "llama3.2";
const modeAliases = {
  kitchen: "home",
  desk: "office",
};
const validModes = new Set(["home", "office"]);
const validStatuses = new Set(["Open", "Done", "Archived"]);

function sendJson(res, statusCode, body) {
  res.writeHead(statusCode, {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET,POST,PUT,DELETE,OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
    "Content-Type": "application/json",
  });
  res.end(JSON.stringify(body, null, 2));
}

function sendError(res, statusCode, message) {
  sendJson(res, statusCode, { error: message });
}

async function readState() {
  return migrateState(JSON.parse(await readFile(statePath, "utf8")));
}

async function writeState(state) {
  state = migrateState(state);
  await writeFile(statePath, `${JSON.stringify(state, null, 2)}\n`, "utf8");
  await writeMarkdownProjection(state);
}

function migrateState(state) {
  return {
    home: state.home || state.kitchen || { notes: [] },
    office: state.office || state.desk || { notes: [] },
  };
}

async function readJsonBody(req) {
  const chunks = [];
  for await (const chunk of req) chunks.push(chunk);
  const raw = Buffer.concat(chunks).toString("utf8").trim();
  return raw ? JSON.parse(raw) : {};
}

function normalizeMode(mode) {
  const requested = mode?.toLowerCase();
  const normalized = modeAliases[requested] || requested;
  return validModes.has(normalized) ? normalized : null;
}

function normalizeNote(input, existing = {}) {
  const text = String(input.text ?? existing.text ?? "").trim();
  if (!text) throw new Error("Note text is required.");

  const status = String(input.status ?? existing.status ?? "Open");
  if (!validStatuses.has(status)) throw new Error("Invalid note status.");

  return {
    id: String(input.id ?? existing.id ?? `note-${Date.now()}`),
    text,
    assignee: String(input.assignee ?? existing.assignee ?? "You"),
    category: String(input.category ?? existing.category ?? "Reminder"),
    due: String(input.due ?? existing.due ?? "Today"),
    pinned: Boolean(input.pinned ?? existing.pinned ?? false),
    status,
  };
}

function dashboardFor(mode, notes) {
  if (mode === "home") {
    return {
      mode,
      now: "Family reset",
      next: "Check kids reminders, groceries, and open household notes.",
      primaryAgent: "Home",
      secondaryAgent: "Execution",
      focusItems: [
        { title: "Kids", detail: "Library books and class bag need a quick check.", tone: "Rose" },
        { title: "Groceries", detail: "Milk, fruit, yogurt, and lunchbox snacks.", tone: "Green" },
        { title: "Household", detail: "Trash tonight. Home cleanup after dinner.", tone: "Amber" },
      ],
      routines: ["School prep", "Home cleanup", "Dinner mode", "Calm music"],
      notes,
    };
  }

  return {
    mode,
    now: "Focus block",
    next: "Review target roles, move one application forward, then capture wins.",
      primaryAgent: "Strategy",
      secondaryAgent: "Career",
    focusItems: [
      { title: "Career", detail: "Pick one PM/EM role and write the positioning angle.", tone: "Blue" },
      { title: "Interview", detail: "Draft one story using STAR and impact notes.", tone: "Rose" },
      { title: "Learning", detail: "Log the next local-model experiment.", tone: "Green" },
    ],
    routines: ["Start focus", "Practice answer", "Draft post", "Weekly review"],
    notes,
  };
}

async function writeMarkdownProjection(state) {
  await mkdir(join(personalOsDir, "home"), { recursive: true });
  await mkdir(join(personalOsDir, "family"), { recursive: true });
  await mkdir(join(personalOsDir, "career"), { recursive: true });

  const homeNotes = state.home?.notes ?? state.kitchen?.notes ?? [];
  const officeNotes = state.office?.notes ?? state.desk?.notes ?? [];

  await Promise.all([
    writeFile(join(personalOsDir, "home", "portal-notes.md"), renderNotesMarkdown("Home Portal Notes", homeNotes), "utf8"),
    writeFile(join(personalOsDir, "family", "portal-reminders.md"), renderNotesMarkdown("Family Portal Reminders", homeNotes.filter((note) => ["Kids", "School"].includes(note.category))), "utf8"),
    writeFile(join(personalOsDir, "home", "groceries.md"), renderNotesMarkdown("Groceries", homeNotes.filter((note) => note.category === "Grocery")), "utf8"),
    writeFile(join(personalOsDir, "career", "portal-notes.md"), renderNotesMarkdown("Office Portal Notes", officeNotes), "utf8"),
  ]);
}

function renderNotesMarkdown(title, notes) {
  const open = notes.filter((note) => note.status === "Open");
  const done = notes.filter((note) => note.status === "Done");
  const archived = notes.filter((note) => note.status === "Archived");

  return [
    `# ${title}`,
    "",
    "<!-- Generated from Portal Hub backend. Edit source notes from Portal or API. -->",
    "",
    "## Open",
    "",
    ...renderNoteLines(open, false),
    "",
    "## Done",
    "",
    ...renderNoteLines(done, true),
    "",
    "## Archived",
    "",
    ...renderNoteLines(archived, false),
    "",
  ].join("\n");
}

function renderNoteLines(notes, checked) {
  if (notes.length === 0) return ["_None_"];

  return notes.map((note) => {
    const checkbox = checked ? "[x]" : "[ ]";
    const pinned = note.pinned ? " pinned:true" : "";
    return `- ${checkbox} ${note.text} @${note.assignee} #${note.category} due:${note.due} id:${note.id}${pinned}`;
  });
}

function summarizeForAgent(agent, mode, notes) {
  const open = notes.filter((note) => note.status === "Open");
  const done = notes.filter((note) => note.status === "Done");
  const pinned = open.filter((note) => note.pinned);
  const dueToday = open.filter((note) => note.due.toLowerCase() === "today");
  const categories = groupBy(open, (note) => note.category);

  if (agent === "ada") {
    const school = categories.School?.length ?? 0;
    const grocery = categories.Grocery?.length ?? 0;
    const food = categories.Food?.length ?? 0;
    return [
      pinned.length ? `Pinned: ${pinned.map((note) => note.text).join("; ")}.` : "No pinned family reminders.",
      dueToday.length ? `${dueToday.length} item(s) are due today.` : "Nothing else is marked due today.",
      `Household scan: ${school} school, ${grocery} grocery, ${food} food note(s).`,
    ].join(" ");
  }

  if (agent === "karen") {
    return [
      `${open.length} open commitment(s), ${done.length} completed recently.`,
      dueToday.length ? `Do today: ${dueToday.map((note) => note.text).join("; ")}.` : "No today-only tasks are blocking you.",
      open.length ? `Next action: ${open[0].text}.` : "Everything visible is clear.",
    ].join(" ");
  }

  if (agent === "hedy") {
    const career = categories.Career?.length ?? 0;
    const applications = categories.Applications?.length ?? 0;
    return `Strategy check: ${career} career note(s), ${applications} application note(s). Pick one role/action to move forward before adding more.`;
  }

  if (agent === "savitri") {
    return "Confidence check: this Portal project is already a strong execution story. Capture the problem, constraints, shipped iterations, and household impact.";
  }

  if (agent === "grace") {
    return "Writing prompt: turn one visible note into a short update with context, decision, and what changed because of it.";
  }

  return mode === "home" ? summarizeForAgent("ada", mode, notes) : summarizeForAgent("karen", mode, notes);
}

function groupBy(items, keyFn) {
  return items.reduce((groups, item) => {
    const key = keyFn(item);
    groups[key] = groups[key] || [];
    groups[key].push(item);
    return groups;
  }, {});
}

async function summarizeWithLocalModel(agent, mode, notes) {
  if (!localModelUrl) return summarizeForAgent(agent, mode, notes);

  const open = notes.filter((note) => note.status === "Open");
  const prompt = [
    `You are ${agent}, one specialist in this Portal dashboard.`,
    `Portal mode: ${mode}.`,
    "Summarize the visible notes in 2-3 concise sentences for a household/career dashboard.",
    "Be practical, kind, and action-oriented. Do not invent facts.",
    "",
    JSON.stringify(open, null, 2),
  ].join("\n");

  return runCatchingAsync(async () => {
    const response = await fetch(localModelUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        model: localModelName,
        prompt,
        stream: false,
      }),
    });

    if (!response.ok) throw new Error(`Local model returned HTTP ${response.status}`);
    const body = await response.json();
    return String(body.response || body.message?.content || "").trim() || summarizeForAgent(agent, mode, notes);
  }, () => summarizeForAgent(agent, mode, notes));
}

async function runCatchingAsync(fn, fallback) {
  try {
    return await fn();
  } catch {
    return fallback();
  }
}

async function handleRequest(req, res) {
  if (req.method === "OPTIONS") {
    sendJson(res, 204, {});
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host}`);
  const parts = url.pathname.split("/").filter(Boolean);

  try {
    if (req.method === "GET" && url.pathname === "/health") {
      sendJson(res, 200, { ok: true });
      return;
    }

    if (req.method === "GET" && parts[0] === "api" && parts[1] === "portal" && parts.length === 3) {
      const mode = normalizeMode(parts[2]);
      if (!mode) return sendError(res, 404, "Unknown portal mode.");
      const state = await readState();
      sendJson(res, 200, dashboardFor(mode, state[mode]?.notes ?? []));
      return;
    }

    if (req.method === "GET" && parts[0] === "api" && parts[1] === "agents" && parts.length === 4 && parts[3] === "summary") {
      const agent = parts[2]?.toLowerCase();
      const mode = normalizeMode(url.searchParams.get("mode")) || "home";
      const state = await readState();
      const notes = state[mode]?.notes ?? [];
      sendJson(res, 200, {
        agent,
        mode,
        source: localModelUrl ? "local-model" : "rules",
        message: await summarizeWithLocalModel(agent, mode, notes),
      });
      return;
    }

    if (req.method === "PUT" && parts[0] === "api" && parts[1] === "portal" && parts[3] === "notes") {
      const mode = normalizeMode(parts[2]);
      if (!mode) return sendError(res, 404, "Unknown portal mode.");
      const body = await readJsonBody(req);
      if (!Array.isArray(body.notes)) return sendError(res, 400, "notes array is required.");
      const state = await readState();
      state[mode].notes = body.notes.map((note) => normalizeNote(note));
      await writeState(state);
      sendJson(res, 200, dashboardFor(mode, state[mode].notes));
      return;
    }

    if (parts[0] === "api" && parts[1] === "notes") {
      const state = await readState();

      if (req.method === "POST" && parts.length === 2) {
        const body = await readJsonBody(req);
        const mode = normalizeMode(body.mode);
        if (!mode) return sendError(res, 400, "Valid mode is required.");
        const note = normalizeNote(body);
        state[mode].notes = [note, ...(state[mode].notes ?? [])];
        await writeState(state);
        sendJson(res, 201, note);
        return;
      }

      if ((req.method === "PUT" || req.method === "DELETE" || req.method === "POST") && parts.length >= 3) {
        const noteId = parts[2];
        const body = await readJsonBody(req);
        const mode = normalizeMode(body.mode);
        if (!mode) return sendError(res, 400, "Valid mode is required.");

        const notes = state[mode].notes ?? [];
        const existing = notes.find((note) => note.id === noteId);
        if (!existing) return sendError(res, 404, "Note not found.");

        if (req.method === "DELETE") {
          state[mode].notes = notes.filter((note) => note.id !== noteId);
          await writeState(state);
          sendJson(res, 200, { ok: true });
          return;
        }

        const statusAction = parts[3];
        const patch =
            statusAction === "done"
              ? { ...body, status: "Done" }
              : statusAction === "archive"
                ? { ...body, status: "Archived" }
                : statusAction === "reopen"
                  ? { ...body, status: "Open" }
                  : body;

        const updated = normalizeNote({ ...existing, ...patch, id: noteId }, existing);
        state[mode].notes = notes.map((note) => (note.id === noteId ? updated : note));
        await writeState(state);
        sendJson(res, 200, updated);
        return;
      }
    }

    sendError(res, 404, "Route not found.");
  } catch (error) {
    sendError(res, 400, error.message || "Bad request.");
  }
}

createServer(handleRequest).listen(port, "0.0.0.0", () => {
  console.log(`Portal Hub backend listening on http://0.0.0.0:${port}`);
});
