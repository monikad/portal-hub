package com.monikabele.portalhub

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monikabele.portalhub.ui.theme.PortalHubTheme
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private enum class PortalMode(val label: String) {
  Home("Home"),
  Office("Office"),
}

private enum class HomeSection(val label: String) {
  Calendar("Calendar"),
  Lists("Lists"),
  Chores("Chores"),
  Notes("Notes"),
}

private data class DashboardState(
    val mode: PortalMode,
    val now: String,
    val next: String,
    val primaryAgent: String,
    val secondaryAgent: String,
    val focusItems: List<FocusItem>,
    val events: List<PortalEvent>,
    val notes: List<PortalNote>,
    val routines: List<String>,
    val musicModes: List<String>,
)

private data class FocusItem(val title: String, val detail: String, val tone: AccentTone)

private data class PortalEvent(val time: String, val title: String, val detail: String)

private data class CalendarFeed(val label: String, val url: String)

private data class PortalNote(
    val id: String,
    val text: String,
    val assignee: String,
    val category: String,
    val due: String,
    val pinned: Boolean = false,
    val status: NoteStatus = NoteStatus.Open,
)

private enum class NoteStatus {
  Open,
  Done,
  Archived,
}

private enum class SyncStatus(val label: String) {
  Local("Local"),
  Syncing("Syncing"),
  Synced("Synced"),
  Offline("Offline"),
}

private enum class AccentTone(val color: Color) {
  Blue(Color(0xFF6FA8A0)),
  Green(Color(0xFF5FAF82)),
  Amber(Color(0xFFD6A64F)),
  Rose(Color(0xFFD98195)),
}

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val appContext = this
    enableEdgeToEdge()
    setContent {
      PortalHubTheme(darkTheme = true) {
        var mode by remember { mutableStateOf(loadPortalMode(appContext)) }
        var showAddNote by remember { mutableStateOf(false) }
        var noteBeingEdited by remember { mutableStateOf<PortalNote?>(null) }
        var syncStatus by remember { mutableStateOf(SyncStatus.Local) }
        var agentMessage by remember { mutableStateOf<String?>(null) }
        var musicMessage by remember { mutableStateOf<String?>(null) }
        var selectedHomeSection by remember { mutableStateOf(HomeSection.Calendar) }
        val notesByMode =
            remember {
              mutableStateMapOf(
                  PortalMode.Home to loadPortalNotes(appContext, PortalMode.Home),
                  PortalMode.Office to loadPortalNotes(appContext, PortalMode.Office),
              )
            }
        val eventsByMode =
            remember {
              mutableStateMapOf(
                  PortalMode.Home to loadPortalEvents(appContext, PortalMode.Home),
                  PortalMode.Office to loadPortalEvents(appContext, PortalMode.Office),
              )
            }
        val state =
            remember(mode, notesByMode[mode], eventsByMode[mode]) {
              dashboardFor(mode)
                  .copy(
                      notes = notesByMode[mode].orEmpty(),
                      events = eventsByMode[mode].orEmpty(),
                  )
            }

        LaunchedEffect(Unit) {
          refreshHomeCalendar(
              context = appContext,
              onSuccess = { events ->
                appContext.runOnUiThread {
                  eventsByMode[PortalMode.Home] = events
                  savePortalEvents(appContext, PortalMode.Home, events)
                }
              },
              onFailure = {
                appContext.runOnUiThread {
                  if (eventsByMode[PortalMode.Home].orEmpty() == dashboardFor(PortalMode.Home).events) {
                    eventsByMode[PortalMode.Home] = calendarSetupEvents()
                  }
                }
              },
          )
        }

        Scaffold(
            topBar = {
              TopAppBar(
                  title = {
                    Column {
                      Text("Portal Hub", fontWeight = FontWeight.Bold)
                      Text(
                          "${state.mode.label} portal",
                          color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                          fontSize = 16.sp,
                      )
                    }
                  },
                  colors =
                      TopAppBarDefaults.topAppBarColors(
                          containerColor = MaterialTheme.colorScheme.primaryContainer
                      ),
              )
            }
        ) { paddingValues ->
          PortalDashboard(
              state = state,
              selectedMode = mode,
              onModeSelected = {
                mode = it
                savePortalMode(appContext, it)
              },
              onAddNoteRequest = { showAddNote = true },
              onEditNote = { noteBeingEdited = it },
              selectedHomeSection = selectedHomeSection,
              onHomeSectionSelected = { selectedHomeSection = it },
              syncStatus = syncStatus,
              onSync = {
                syncStatus = SyncStatus.Syncing
                if (mode == PortalMode.Home) {
                  refreshHomeCalendar(
                      context = appContext,
                      onSuccess = { events ->
                        appContext.runOnUiThread {
                          eventsByMode[PortalMode.Home] = events
                          savePortalEvents(appContext, PortalMode.Home, events)
                        }
                      },
                      onFailure = {
                        appContext.runOnUiThread {
                          if (eventsByMode[PortalMode.Home].orEmpty() == dashboardFor(PortalMode.Home).events) {
                            eventsByMode[PortalMode.Home] = calendarSetupEvents()
                          }
                        }
                      },
                  )
                }
                syncPortalNotes(
                    mode = mode,
                    notes = notesByMode[mode].orEmpty(),
                    onSuccess = { syncedNotes ->
                      appContext.runOnUiThread {
                        notesByMode[mode] = syncedNotes
                        savePortalNotes(appContext, mode, syncedNotes)
                        syncStatus = SyncStatus.Synced
                      }
                    },
                    onFailure = { appContext.runOnUiThread { syncStatus = SyncStatus.Offline } },
                )
              },
              agentMessage = agentMessage,
              onAskAgent = { agent ->
                agentMessage = "Asking $agent..."
                fetchAgentSummary(
                    agent = agent,
                    mode = mode,
                    onSuccess = { message -> appContext.runOnUiThread { agentMessage = message } },
                    onFailure = { appContext.runOnUiThread { agentMessage = "$agent is offline. Try Sync when the backend is running." } },
                )
              },
              musicMessage = musicMessage,
              onMusicModeSelected = { musicMode ->
                musicMessage = musicSuggestionFor(musicMode)
              },
              onCompleteNote = { completedNote ->
                val updatedNotes =
                    notesByMode[mode].orEmpty().map { note ->
                      if (note.id == completedNote.id) note.copy(status = NoteStatus.Done) else note
                    }
                notesByMode[mode] = updatedNotes
                savePortalNotes(appContext, mode, updatedNotes)
                syncStatus = SyncStatus.Local
              },
              onReopenNote = { reopenedNote ->
                val updatedNotes =
                    notesByMode[mode].orEmpty().map { note ->
                      if (note.id == reopenedNote.id) note.copy(status = NoteStatus.Open) else note
                    }
                notesByMode[mode] = updatedNotes
                savePortalNotes(appContext, mode, updatedNotes)
                syncStatus = SyncStatus.Local
              },
              modifier = Modifier.padding(paddingValues),
          )
        }

        if (showAddNote) {
          NoteEditorDialog(
              mode = mode,
              note = null,
              plainNote = mode == PortalMode.Home && selectedHomeSection == HomeSection.Notes,
              onDismiss = { showAddNote = false },
              onSave = { note ->
                val newNotes = expandSavedNote(note)
                val updatedNotes = newNotes + notesByMode[mode].orEmpty()
                notesByMode[mode] = updatedNotes
                savePortalNotes(appContext, mode, updatedNotes)
                syncStatus = SyncStatus.Local
                showAddNote = false
              },
              onArchive = null,
              onDelete = null,
          )
        }

        noteBeingEdited?.let { existingNote ->
          NoteEditorDialog(
              mode = mode,
              note = existingNote,
              plainNote = existingNote.isGeneralNote(),
              onDismiss = { noteBeingEdited = null },
              onSave = { updatedNote ->
                val updatedNotes =
                    notesByMode[mode].orEmpty().map { note ->
                      if (note.id == updatedNote.id) updatedNote else note
                    }
                notesByMode[mode] = updatedNotes
                savePortalNotes(appContext, mode, updatedNotes)
                syncStatus = SyncStatus.Local
                noteBeingEdited = null
              },
              onArchive = {
                val updatedNotes =
                    notesByMode[mode].orEmpty().map { note ->
                      if (note.id == existingNote.id) note.copy(status = NoteStatus.Archived) else note
                    }
                notesByMode[mode] = updatedNotes
                savePortalNotes(appContext, mode, updatedNotes)
                syncStatus = SyncStatus.Local
                noteBeingEdited = null
              },
              onDelete = {
                val updatedNotes = notesByMode[mode].orEmpty().filterNot { it.id == existingNote.id }
                notesByMode[mode] = updatedNotes
                savePortalNotes(appContext, mode, updatedNotes)
                syncStatus = SyncStatus.Local
                noteBeingEdited = null
              },
          )
        }
      }
    }
  }
}

@Composable
private fun PortalDashboard(
    state: DashboardState,
    selectedMode: PortalMode,
    onModeSelected: (PortalMode) -> Unit,
    onAddNoteRequest: () -> Unit,
    onEditNote: (PortalNote) -> Unit,
    selectedHomeSection: HomeSection,
    onHomeSectionSelected: (HomeSection) -> Unit,
    syncStatus: SyncStatus,
    onSync: () -> Unit,
    agentMessage: String?,
    onAskAgent: (String) -> Unit,
    musicMessage: String?,
    onMusicModeSelected: (String) -> Unit,
    onCompleteNote: (PortalNote) -> Unit,
    onReopenNote: (PortalNote) -> Unit,
    modifier: Modifier = Modifier,
) {
  Column(
      modifier =
          modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .verticalScroll(rememberScrollState())
              .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    ModeSwitch(selectedMode = selectedMode, onModeSelected = onModeSelected)
    if (state.mode == PortalMode.Home) {
      HomeCommandCenter(
          state = state,
          selectedHomeSection = selectedHomeSection,
          onHomeSectionSelected = onHomeSectionSelected,
          syncStatus = syncStatus,
          onAddNoteRequest = onAddNoteRequest,
          onSync = onSync,
          onEditNote = onEditNote,
          onCompleteNote = onCompleteNote,
          onReopenNote = onReopenNote,
      )
    } else {
      SchedulePanel(events = state.events)
      NotesPanel(
          notes = state.notes,
          syncStatus = syncStatus,
          plainNotesOnly = false,
          onAddNoteRequest = onAddNoteRequest,
          onSync = onSync,
          onEditNote = onEditNote,
          onCompleteNote = onCompleteNote,
      )
      FocusGrid(items = state.focusItems)
      NowPanel(state = state)
      AgentActions(
          primaryAgent = state.primaryAgent,
          secondaryAgent = state.secondaryAgent,
          agentMessage = agentMessage,
          onAskAgent = onAskAgent,
          onAddNoteRequest = onAddNoteRequest,
      )
      RoutineActions(
          routines = state.routines,
          musicModes = state.musicModes,
          musicMessage = musicMessage,
          onMusicModeSelected = onMusicModeSelected,
      )
    }
  }
}

@Composable
private fun HomeCommandCenter(
    state: DashboardState,
    selectedHomeSection: HomeSection,
    onHomeSectionSelected: (HomeSection) -> Unit,
    syncStatus: SyncStatus,
    onAddNoteRequest: () -> Unit,
    onSync: () -> Unit,
    onEditNote: (PortalNote) -> Unit,
    onCompleteNote: (PortalNote) -> Unit,
    onReopenNote: (PortalNote) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    HomeSectionTabs(selected = selectedHomeSection, onSelected = onHomeSectionSelected)
    when (selectedHomeSection) {
      HomeSection.Calendar -> {
        SchedulePanel(events = state.events)
        HomeSummaryStrip(notes = state.notes, events = state.events)
        FocusGrid(items = state.focusItems)
      }
      HomeSection.Lists ->
          ListsPanel(
              notes = state.notes,
              onAddNoteRequest = onAddNoteRequest,
              onEditNote = onEditNote,
              onCompleteNote = onCompleteNote,
              onReopenNote = onReopenNote,
          )
      HomeSection.Chores ->
          ChoresPanel(
              notes = state.notes,
              onAddNoteRequest = onAddNoteRequest,
              onEditNote = onEditNote,
              onCompleteNote = onCompleteNote,
              onReopenNote = onReopenNote,
          )
      HomeSection.Notes ->
          NotesPanel(
              notes = state.notes,
              syncStatus = syncStatus,
              plainNotesOnly = true,
              onAddNoteRequest = onAddNoteRequest,
              onSync = onSync,
              onEditNote = onEditNote,
              onCompleteNote = onCompleteNote,
          )
    }
  }
}

@Composable
private fun HomeSectionTabs(selected: HomeSection, onSelected: (HomeSection) -> Unit) {
  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    HomeSection.entries.forEach { section ->
      FilterChip(
          selected = selected == section,
          onClick = { onSelected(section) },
          label = { Text(section.label, fontSize = 19.sp, fontWeight = FontWeight.SemiBold) },
          modifier = Modifier.height(54.dp),
          colors =
              FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              ),
      )
    }
  }
}

@Composable
private fun HomeSummaryStrip(notes: List<PortalNote>, events: List<PortalEvent>) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
    SummaryTile("Events", events.size.toString(), "visible", AccentTone.Amber, Modifier.weight(1f))
    SummaryTile("Open", notes.count { it.status == NoteStatus.Open }.toString(), "notes", AccentTone.Green, Modifier.weight(1f))
    SummaryTile("Pinned", notes.count { it.pinned && it.status == NoteStatus.Open }.toString(), "important", AccentTone.Rose, Modifier.weight(1f))
  }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    detail: String,
    tone: AccentTone,
    modifier: Modifier = Modifier,
) {
  Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, modifier = modifier.height(104.dp)) {
    Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(tone.color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f), fontSize = 17.sp, maxLines = 1)
      }
      Text(value, fontSize = 30.sp, fontWeight = FontWeight.Bold)
      Text(detail, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f), fontSize = 16.sp, maxLines = 1)
    }
  }
}

@Composable
private fun ModeSwitch(selectedMode: PortalMode, onModeSelected: (PortalMode) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
    PortalMode.entries.forEach { mode ->
      FilterChip(
          selected = selectedMode == mode,
          onClick = { onModeSelected(mode) },
          label = {
            Text(
                mode.label,
                fontSize = 22.sp,
                fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Medium,
            )
          },
          modifier = Modifier.weight(1f).height(64.dp),
          colors =
              FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primary,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
              ),
      )
    }
  }
}

@Composable
private fun NowPanel(state: DashboardState) {
  Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Text("Now", color = MaterialTheme.colorScheme.secondary, fontSize = 22.sp)
      Text(state.now, fontSize = 42.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold)
      Text(state.next, fontSize = 24.sp, lineHeight = 30.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
    }
  }
}

@Composable
private fun FocusGrid(items: List<FocusItem>) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionTitle("Today")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      items.take(2).forEach { item -> FocusCard(item = item, modifier = Modifier.weight(1f)) }
    }
    if (items.size > 2) {
      FocusCard(item = items[2], modifier = Modifier.fillMaxWidth())
    }
  }
}

@Composable
private fun SchedulePanel(events: List<PortalEvent>) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionTitle("Schedule")
    if (events.isEmpty()) {
      Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
        Text(
            "No scheduled items are visible for this portal.",
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            fontSize = 21.sp,
        )
      }
    } else {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        events.take(2).forEach { event -> EventCard(event = event, modifier = Modifier.weight(1f)) }
      }
      events.drop(2).forEach { event -> EventCard(event = event, modifier = Modifier.fillMaxWidth()) }
    }
  }
}

@Composable
private fun EventCard(event: PortalEvent, modifier: Modifier = Modifier) {
  Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, modifier = modifier.height(118.dp)) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(event.time, color = MaterialTheme.colorScheme.secondary, fontSize = 19.sp, fontWeight = FontWeight.Bold)
      Text(event.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Text(event.detail, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
private fun FocusCard(item: FocusItem, modifier: Modifier = Modifier) {
  Card(
      modifier = modifier.height(152.dp),
      shape = RoundedCornerShape(8.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(item.tone.color))
        Spacer(modifier = Modifier.width(10.dp))
        Text(item.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      Text(item.detail, fontSize = 20.sp, lineHeight = 25.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f))
    }
  }
}

@Composable
private fun NotesPanel(
    notes: List<PortalNote>,
    syncStatus: SyncStatus,
    plainNotesOnly: Boolean,
    onAddNoteRequest: () -> Unit,
    onSync: () -> Unit,
    onEditNote: (PortalNote) -> Unit,
    onCompleteNote: (PortalNote) -> Unit,
) {
  val visibleNotes =
      if (plainNotesOnly) {
        notes.filter { it.status != NoteStatus.Archived && it.isGeneralNote() }
      } else {
        notes.filter { it.status == NoteStatus.Open }
      }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
      Column {
        SectionTitle("Notes")
        Text(
            if (plainNotesOnly) "${visibleNotes.size} saved notes" else "${visibleNotes.size} open notes",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            fontSize = 18.sp,
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            syncStatus.label,
            color = syncStatus.color(),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        OutlinedButton(onClick = onSync, modifier = Modifier.height(58.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
          Text("Sync", fontSize = 20.sp)
        }
        OutlinedButton(onClick = onAddNoteRequest, modifier = Modifier.height(58.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
          Text("+ Note", fontSize = 20.sp)
        }
      }
    }
    if (visibleNotes.isEmpty()) {
      EmptyNotesRow()
    } else {
      visibleNotes.forEach { note ->
        NoteRow(note = note, onEditNote = onEditNote, onCompleteNote = onCompleteNote)
      }
    }
  }
}

@Composable
private fun ListsPanel(
    notes: List<PortalNote>,
    onAddNoteRequest: () -> Unit,
    onEditNote: (PortalNote) -> Unit,
    onCompleteNote: (PortalNote) -> Unit,
    onReopenNote: (PortalNote) -> Unit,
) {
  val visibleNotes = notes.filter { it.status != NoteStatus.Archived }
  val listNotes = visibleNotes.filterNot { it.isChoreNote() }
  val grocery = listNotes.filter { it.category == "Grocery" }
  val school = listNotes.filter { it.category == "School" || it.category == "Kids" }
  val other = listNotes.filterNot { it in grocery || it in school }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    PanelHeader(
        title = "Lists",
        detail = "Groceries, school, household, and quick reminders",
        actionLabel = "+ Item",
        onAction = onAddNoteRequest,
    )
    ListBucket(
        title = "Grocery",
        accent = AccentTone.Green,
        notes = grocery,
        emptyText = "No grocery items yet.",
        onEditNote = onEditNote,
        onCompleteNote = onCompleteNote,
        onReopenNote = onReopenNote,
    )
    ListBucket(
        title = "School & Kids",
        accent = AccentTone.Rose,
        notes = school,
        emptyText = "No school or kids reminders.",
        onEditNote = onEditNote,
        onCompleteNote = onCompleteNote,
        onReopenNote = onReopenNote,
    )
    if (other.isNotEmpty()) {
      ListBucket(
          title = "Other",
          accent = AccentTone.Blue,
          notes = other,
          emptyText = "",
          onEditNote = onEditNote,
          onCompleteNote = onCompleteNote,
          onReopenNote = onReopenNote,
      )
    }
  }
}

@Composable
private fun ChoresPanel(
    notes: List<PortalNote>,
    onAddNoteRequest: () -> Unit,
    onEditNote: (PortalNote) -> Unit,
    onCompleteNote: (PortalNote) -> Unit,
    onReopenNote: (PortalNote) -> Unit,
) {
  val visibleNotes = notes.filter { it.status != NoteStatus.Archived && it.isChoreNote() }
  val people = listOf("You", "Partner", "Child 1", "Child 2", "Helper")

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    PanelHeader(
        title = "Chores",
        detail = "A simple family board for morning, evening, and household tasks",
        actionLabel = "+ Chore",
        onAction = onAddNoteRequest,
    )
    people.forEachIndexed { index, person ->
      val personNotes = visibleNotes.filter { it.assignee == person }
      ChorePersonCard(
          person = person,
          accent = AccentTone.entries[index % AccentTone.entries.size],
          notes = personNotes,
          onEditNote = onEditNote,
          onCompleteNote = onCompleteNote,
          onReopenNote = onReopenNote,
      )
    }
  }
}

@Composable
private fun PanelHeader(title: String, detail: String, actionLabel: String, onAction: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      SectionTitle(title)
      Text(detail, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f), fontSize = 18.sp, lineHeight = 23.sp)
    }
    OutlinedButton(onClick = onAction, modifier = Modifier.height(58.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
      Text(actionLabel, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
  }
}

@Composable
private fun ListBucket(
    title: String,
    accent: AccentTone,
    notes: List<PortalNote>,
    emptyText: String,
    onEditNote: (PortalNote) -> Unit,
    onCompleteNote: (PortalNote) -> Unit,
    onReopenNote: (PortalNote) -> Unit,
) {
  Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(accent.color))
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            "${notes.count { it.status == NoteStatus.Open }}/${notes.size}",
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
      }
      if (notes.isEmpty()) {
        Text(emptyText, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 19.sp)
      } else {
        notes.forEach { note ->
          CompactTaskRow(note = note, onEditNote = onEditNote, onCompleteNote = onCompleteNote, onReopenNote = onReopenNote)
        }
      }
    }
  }
}

@Composable
private fun ChorePersonCard(
    person: String,
    accent: AccentTone,
    notes: List<PortalNote>,
    onEditNote: (PortalNote) -> Unit,
    onCompleteNote: (PortalNote) -> Unit,
    onReopenNote: (PortalNote) -> Unit,
) {
  Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(accent.color), contentAlignment = Alignment.Center) {
          Text(person.take(1), color = Color(0xFF151515), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(person, fontSize = 24.sp, fontWeight = FontWeight.Bold)
          Text(
              "${notes.count { it.status == NoteStatus.Open }} open | ${notes.count { it.status == NoteStatus.Done }} done",
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
              fontSize = 17.sp,
          )
        }
      }
      if (notes.isEmpty()) {
        Text("No tasks assigned.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), fontSize = 19.sp)
      } else {
        notes.forEach { note ->
          CompactTaskRow(note = note, onEditNote = onEditNote, onCompleteNote = onCompleteNote, onReopenNote = onReopenNote)
        }
      }
    }
  }
}

@Composable
private fun CompactTaskRow(
    note: PortalNote,
    onEditNote: (PortalNote) -> Unit,
    onCompleteNote: (PortalNote) -> Unit,
    onReopenNote: (PortalNote) -> Unit,
) {
  val isDone = note.status == NoteStatus.Done
  Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Checkbox(
        checked = isDone,
        onCheckedChange = { isChecked ->
          if (isChecked) {
            onCompleteNote(note)
          } else {
            onReopenNote(note)
          }
        },
        modifier = Modifier.size(46.dp),
        colors =
            CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.secondary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
          note.text,
          fontSize = 20.sp,
          fontWeight = FontWeight.SemiBold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDone) 0.48f else 1f),
      )
      Text(
          "${note.assignee} | ${note.due}",
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDone) 0.42f else 0.62f),
          fontSize = 16.sp,
          maxLines = 1,
      )
    }
    TextButton(onClick = { onEditNote(note) }, modifier = Modifier.height(46.dp)) {
      Text("Edit", fontSize = 16.sp)
    }
  }
}

@Composable
private fun EmptyNotesRow() {
  Surface(
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surface,
  ) {
    Text(
        "All clear. Add a note, grocery item, or reminder when something comes up.",
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        fontSize = 21.sp,
        lineHeight = 28.sp,
    )
  }
}

@Composable
private fun RecentlyDoneNotes(notes: List<PortalNote>, onReopenNote: (PortalNote) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
    Text(
        "Done recently",
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
    )
    notes.forEach { note ->
      Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(note.text, modifier = Modifier.weight(1f), fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
          OutlinedButton(onClick = { onReopenNote(note) }, modifier = Modifier.height(52.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
            Text("Undo", fontSize = 17.sp)
          }
        }
      }
    }
  }
}

@Composable
private fun NoteRow(
    note: PortalNote,
    onEditNote: (PortalNote) -> Unit,
    onCompleteNote: (PortalNote) -> Unit,
) {
  val isGeneralNote = note.isGeneralNote()
  Card(
      shape = RoundedCornerShape(8.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
          modifier =
              Modifier
                  .size(46.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (note.pinned) Color(0xFFE6A23C) else AccentTone.Blue.color),
          contentAlignment = Alignment.Center,
      ) {
        Text(if (note.pinned) "!" else "N", color = Color(0xFF101010), fontSize = 22.sp, fontWeight = FontWeight.Bold)
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(note.text, fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
        if (!isGeneralNote) {
          Text(
              "${note.category} | ${note.assignee} | ${note.due}",
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
              fontSize = 18.sp,
          )
        }
      }
      OutlinedButton(onClick = { onEditNote(note) }, modifier = Modifier.height(56.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
        Text("Edit", fontSize = 18.sp)
      }
      if (!isGeneralNote) {
        Button(onClick = { onCompleteNote(note) }, modifier = Modifier.height(56.dp), contentPadding = PaddingValues(horizontal = 18.dp)) {
          Text("Done", fontSize = 18.sp)
        }
      }
    }
  }
}

@Composable
private fun AgentActions(
    primaryAgent: String,
    secondaryAgent: String,
    agentMessage: String?,
    onAskAgent: (String) -> Unit,
    onAddNoteRequest: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionTitle("Ask")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      ActionButton(label = "Ask $primaryAgent", modifier = Modifier.weight(1f), onClick = { onAskAgent(primaryAgent) })
      ActionButton(label = "Ask $secondaryAgent", modifier = Modifier.weight(1f), onClick = { onAskAgent(secondaryAgent) })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
      ActionButton(label = "What is next?", modifier = Modifier.weight(1f), primary = false)
      ActionButton(label = "Add reminder", modifier = Modifier.weight(1f), primary = false, onClick = onAddNoteRequest)
    }
    agentMessage?.let { message ->
      Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            fontSize = 21.sp,
            lineHeight = 28.sp,
        )
      }
    }
  }
}

@Composable
private fun RoutineActions(
    routines: List<String>,
    musicModes: List<String>,
    musicMessage: String?,
    onMusicModeSelected: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    SectionTitle("Routines")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      routines.forEach { routine ->
        ActionButton(label = routine, width = 232.dp, primary = false)
      }
    }
    SectionTitle("Music")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      musicModes.forEach { musicMode ->
        ActionButton(label = musicMode, width = 232.dp, primary = false, onClick = { onMusicModeSelected(musicMode) })
      }
    }
    musicMessage?.let { message ->
      Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            fontSize = 21.sp,
            lineHeight = 28.sp,
        )
      }
    }
  }
}

@Composable
private fun ActionButton(
    label: String,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp? = null,
    primary: Boolean = true,
    onClick: () -> Unit = {},
) {
  val buttonModifier = (width?.let { Modifier.width(it) } ?: modifier).height(72.dp)
  if (primary) {
    Button(onClick = onClick, modifier = buttonModifier, shape = RoundedCornerShape(8.dp)) {
      Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  } else {
    OutlinedButton(onClick = onClick, modifier = buttonModifier, shape = RoundedCornerShape(8.dp)) {
      Text(label, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Composable
private fun NoteEditorDialog(
    mode: PortalMode,
    note: PortalNote?,
    plainNote: Boolean,
    onDismiss: () -> Unit,
    onSave: (PortalNote) -> Unit,
    onArchive: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
  var text by remember(note) { mutableStateOf(note?.text.orEmpty()) }
  var assignee by remember(note, plainNote) { mutableStateOf(note?.assignee ?: if (plainNote) "" else "You") }
  var category by remember(note, plainNote) { mutableStateOf(note?.category ?: if (plainNote) "Note" else if (mode == PortalMode.Home) "Grocery" else "Career") }
  var due by remember(note, plainNote) { mutableStateOf(note?.due ?: if (plainNote) "" else "Today") }
  val canSave = text.trim().isNotEmpty()
  val isEditing = note != null

  AlertDialog(
      onDismissRequest = onDismiss,
      title = {
        Text(
            if (isEditing) "Edit note" else "Add note",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          OutlinedTextField(
              value = text,
              onValueChange = { text = it },
              label = { Text("Note") },
              singleLine = false,
              minLines = if (plainNote) 7 else 2,
              textStyle = androidx.compose.ui.text.TextStyle(fontSize = 22.sp),
              modifier = Modifier.fillMaxWidth(),
          )
          if (!plainNote) {
            QuickChoiceRow(
                label = "Person",
                options = if (mode == PortalMode.Home) listOf("You", "Partner", "Child 1", "Child 2", "Helper") else listOf("You", "Execution", "Writer", "Strategy", "Career"),
                selected = assignee,
                onSelected = { assignee = it },
            )
            QuickChoiceRow(
                label = "Category",
                options = if (mode == PortalMode.Home) listOf("Grocery", "School", "Food", "Kids", "Household") else listOf("Career", "Writing", "Applications", "Learning", "Reminder"),
                selected = category,
                onSelected = { category = it },
            )
            QuickChoiceRow(
                label = "Due",
                options = listOf("Today", "Tomorrow", "This week", "Someday"),
                selected = due,
                onSelected = { due = it },
            )
          }
        }
      },
      confirmButton = {
        Button(
            onClick = {
              if (canSave) {
                onSave(
                    PortalNote(
                        id = note?.id ?: "note-${System.currentTimeMillis()}",
                        text = text.trim(),
                        assignee = if (plainNote) "" else assignee,
                        category = if (plainNote) "Note" else category,
                        due = if (plainNote) "" else due,
                        pinned = note?.pinned ?: false,
                        status = note?.status ?: NoteStatus.Open,
                    )
                )
              }
            },
            enabled = canSave,
            modifier = Modifier.height(58.dp),
        ) {
          Text(if (isEditing) "Update" else "Save", fontSize = 19.sp)
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (isEditing) {
            TextButton(onClick = { onArchive?.invoke() }, modifier = Modifier.height(58.dp)) {
              Text("Archive", fontSize = 18.sp)
            }
            TextButton(onClick = { onDelete?.invoke() }, modifier = Modifier.height(58.dp)) {
              Text("Delete", color = Color(0xFFFF8A8A), fontSize = 18.sp)
            }
          }
          TextButton(onClick = onDismiss, modifier = Modifier.height(58.dp)) {
            Text("Cancel", fontSize = 19.sp)
          }
        }
      },
  )
}

@Composable
private fun QuickChoiceRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 18.sp)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      options.forEach { option ->
        FilterChip(
            selected = option == selected,
            onClick = { onSelected(option) },
            label = { Text(option, fontSize = 17.sp) },
            colors =
                FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
        )
      }
    }
  }
}

@Composable
private fun SectionTitle(label: String) {
  Text(label, fontSize = 27.sp, fontWeight = FontWeight.Bold)
}

private fun dashboardFor(mode: PortalMode): DashboardState =
    when (mode) {
      PortalMode.Home ->
          DashboardState(
              mode = mode,
              now = "Family reset",
              next = "Check kids reminders, groceries, and open household notes.",
              primaryAgent = "Home",
              secondaryAgent = "Execution",
              focusItems =
                  listOf(
                      FocusItem("Kids", "Library books and class bag need a quick check.", AccentTone.Rose),
                      FocusItem("Groceries", "Milk, fruit, yogurt, and lunchbox snacks.", AccentTone.Green),
                      FocusItem("Household", "Trash tonight. Home cleanup after dinner.", AccentTone.Amber),
                  ),
              events =
                  listOf(
                      PortalEvent("Today", "School bag check", "Folders, books, forms, and class supplies."),
                      PortalEvent("Evening", "Home reset", "Groceries, dinner cleanup, and tomorrow prep."),
                      PortalEvent("This week", "Kids classes", "Use this surface for class reminders and pickup notes."),
                  ),
              notes =
                  listOf(
                      PortalNote("home-school-folder", "Bring blue folder tomorrow", "Child 1", "School", "Tomorrow", pinned = true),
                      PortalNote("home-grocery-yogurt", "Buy strawberries and yogurt", "You", "Grocery", "Today"),
                      PortalNote("home-food-dal", "Prep dinner before lunch", "Helper", "Food", "Today"),
                  ),
              routines = listOf("School prep", "Home cleanup", "Dinner mode", "Calm music"),
              musicModes = listOf("Cooking", "Cleaning", "Kids calm", "Dinner"),
          )

      PortalMode.Office ->
          DashboardState(
              mode = mode,
              now = "Focus block",
              next = "Review target roles, move one application forward, then capture wins.",
              primaryAgent = "Strategy",
              secondaryAgent = "Career",
              focusItems =
                  listOf(
                      FocusItem("Career", "Pick one PM/EM role and write the positioning angle.", AccentTone.Blue),
                      FocusItem("Interview", "Draft one story using STAR and impact notes.", AccentTone.Rose),
                      FocusItem("Learning", "Log the next local-model experiment.", AccentTone.Green),
                  ),
              events =
                  listOf(
                      PortalEvent("Now", "Focus block", "Move one career or learning item forward."),
                      PortalEvent("Later", "Application review", "Check shortlist and next follow-up."),
                      PortalEvent("Weekly", "Narrative review", "Update impact log and interview stories."),
                  ),
              notes =
                  listOf(
                      PortalNote("office-career-story", "Turn a useful side project into an interview story", "You", "Career", "This week", pinned = true),
                      PortalNote("office-writing-linkedin", "Draft a short post from this project", "Writer", "Writing", "Friday"),
                      PortalNote("office-applications-followup", "Follow up on target company shortlist", "Execution", "Applications", "Tomorrow"),
                  ),
              routines = listOf("Start focus", "Practice answer", "Draft post", "Weekly review"),
              musicModes = listOf("Focus", "Deep work", "Writing", "Reset"),
          )
    }

private fun musicSuggestionFor(mode: String): String =
    when (mode) {
      "Cooking" -> "Cooking mode: play something warm and steady. Suggested vibe: acoustic, mellow dinner prep, or familiar comfort songs."
      "Cleaning" -> "Cleaning mode: choose upbeat, familiar songs with a clear beat. Keep it energetic but not chaotic."
      "Kids calm" -> "Kids calm mode: soft instrumental, gentle Disney piano, or low-volume bedtime-style music."
      "Dinner" -> "Dinner mode: relaxed family playlist, low vocals, easy conversation volume."
      "Focus" -> "Focus mode: instrumental, low lyrics, 45-60 minute playlist."
      "Deep work" -> "Deep work mode: ambient electronic or brown noise. Start with one task, one timer."
      "Writing" -> "Writing mode: soft piano or lo-fi. Keep it repetitive enough to disappear."
      "Reset" -> "Reset mode: one calm playlist, one small cleanup, one next action."
      else -> "$mode mode selected. Pick music that matches the room and the next action."
    }

private fun expandSavedNote(note: PortalNote): List<PortalNote> {
  if (note.category != "Grocery") return listOf(note)

  val groceryItems =
      note.text
          .split(',')
          .map { it.trim() }
          .filter { it.isNotEmpty() }

  if (groceryItems.size <= 1) return listOf(note)

  return groceryItems.mapIndexed { index, item ->
    note.copy(id = "${note.id}-$index", text = item)
  }
}

private fun PortalNote.isChoreNote(): Boolean =
    category == "Household" || category == "Food"

private fun PortalNote.isGeneralNote(): Boolean =
    category == "Note" || category == "General"

private const val NOTES_PREFS = "personal_os_portal_notes"
private const val EVENTS_PREFS = "personal_os_portal_events"
private const val PORTAL_SETTINGS_PREFS = "personal_os_portal_settings"
private const val SELECTED_MODE_KEY = "selected_mode"
private const val CALENDAR_FEEDS_ASSET = "calendar-feeds.local.json"

private fun loadPortalNotes(context: Context, mode: PortalMode): List<PortalNote> {
  val seedNotes = dashboardFor(mode).notes
  val prefs = context.getSharedPreferences(NOTES_PREFS, Context.MODE_PRIVATE)
  val json = prefs.getString(mode.storageKey(), null) ?: mode.legacyStorageKey()?.let { prefs.getString(it, null) }
  if (json.isNullOrBlank()) return seedNotes

  return runCatching {
        val notes = JSONArray(json)
        List(notes.length()) { index -> notes.getJSONObject(index).toPortalNote() }
      }
      .getOrDefault(seedNotes)
}

private fun savePortalNotes(context: Context, mode: PortalMode, notes: List<PortalNote>) {
  val json = JSONArray()
  notes.forEach { note -> json.put(note.toJson()) }
  context
      .getSharedPreferences(NOTES_PREFS, Context.MODE_PRIVATE)
      .edit()
      .putString(mode.storageKey(), json.toString())
      .apply()
}

private fun loadPortalEvents(context: Context, mode: PortalMode): List<PortalEvent> {
  val seedEvents =
      if (mode == PortalMode.Home && loadCalendarFeeds(context).isEmpty()) {
        calendarSetupEvents()
      } else {
        dashboardFor(mode).events
      }
  val json =
      context
          .getSharedPreferences(EVENTS_PREFS, Context.MODE_PRIVATE)
          .getString(mode.storageKey(), null)
  if (json.isNullOrBlank()) return seedEvents

  return runCatching {
        val events = JSONArray(json)
        List(events.length()) { index -> events.getJSONObject(index).toPortalEvent() }
      }
      .getOrDefault(seedEvents)
}

private fun savePortalEvents(context: Context, mode: PortalMode, events: List<PortalEvent>) {
  val json = JSONArray()
  events.forEach { event -> json.put(event.toJson()) }
  context
      .getSharedPreferences(EVENTS_PREFS, Context.MODE_PRIVATE)
      .edit()
      .putString(mode.storageKey(), json.toString())
      .apply()
}

private fun calendarSetupEvents(): List<PortalEvent> =
    listOf(
        PortalEvent("Calendar", "Add calendar feed", "Use calendar-feeds.local.json for Apple or Google"),
        PortalEvent("Sync", "Tap Sync after install", "Rebuild the APK after editing the feed file"),
    )

private fun loadPortalMode(context: Context): PortalMode {
  val savedMode =
      context
          .getSharedPreferences(PORTAL_SETTINGS_PREFS, Context.MODE_PRIVATE)
          .getString(SELECTED_MODE_KEY, PortalMode.Home.name)
  return when (savedMode) {
    "Kitchen" -> PortalMode.Home
    "Desk" -> PortalMode.Office
    else -> runCatching { PortalMode.valueOf(savedMode ?: PortalMode.Home.name) }.getOrDefault(PortalMode.Home)
  }
}

private fun savePortalMode(context: Context, mode: PortalMode) {
  context
      .getSharedPreferences(PORTAL_SETTINGS_PREFS, Context.MODE_PRIVATE)
      .edit()
      .putString(SELECTED_MODE_KEY, mode.name)
      .apply()
}

private fun PortalMode.storageKey(): String = "notes_${name.lowercase()}"

private fun PortalMode.legacyStorageKey(): String? =
    when (this) {
      PortalMode.Home -> "notes_kitchen"
      PortalMode.Office -> "notes_desk"
    }

private fun PortalNote.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("text", text)
        .put("assignee", assignee)
        .put("category", category)
        .put("due", due)
        .put("pinned", pinned)
        .put("status", status.name)

private fun JSONObject.toPortalNote(): PortalNote =
    PortalNote(
        id = optString("id", "note-${System.currentTimeMillis()}"),
        text = optString("text"),
        assignee = optString("assignee", "You"),
        category = optString("category", "Reminder"),
        due = optString("due", "Today"),
        pinned = optBoolean("pinned", false),
        status = runCatching { NoteStatus.valueOf(optString("status", NoteStatus.Open.name)) }.getOrDefault(NoteStatus.Open),
    )

private fun PortalEvent.toJson(): JSONObject =
    JSONObject()
        .put("time", time)
        .put("title", title)
        .put("detail", detail)

private fun JSONObject.toPortalEvent(): PortalEvent =
    PortalEvent(
        time = optString("time"),
        title = optString("title"),
        detail = optString("detail"),
    )

private const val BACKEND_BASE_URL = "http://127.0.0.1:8787"

private fun refreshHomeCalendar(context: Context, onSuccess: (List<PortalEvent>) -> Unit, onFailure: () -> Unit) {
  val feeds = loadCalendarFeeds(context)
  if (feeds.isEmpty()) {
    onFailure()
    return
  }

  Thread {
        runCatching {
              feeds.flatMap { feed ->
                val feedUrl = feed.url.replace("webcal://", "https://")
                val connection = (URL(feedUrl).openConnection() as HttpURLConnection).apply {
                  requestMethod = "GET"
                  connectTimeout = 4000
                  readTimeout = 6000
                }
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) error("${feed.label} calendar failed with HTTP $responseCode")
                val ics = connection.inputStream.bufferedReader().use { it.readText() }
                parseIcsEvents(ics, feed.label)
              }
            }
            .onSuccess { events ->
              if (events.isNotEmpty()) {
                onSuccess(events.take(8))
              } else {
                onFailure()
              }
            }
            .onFailure { onFailure() }
      }
      .start()
}

private fun loadCalendarFeeds(context: Context): List<CalendarFeed> =
    runCatching {
          val json = context.assets.open(CALENDAR_FEEDS_ASSET).bufferedReader().use { it.readText() }
          val feeds = JSONObject(json).optJSONArray("feeds") ?: JSONArray()
          List(feeds.length()) { index ->
                val feed = feeds.getJSONObject(index)
                CalendarFeed(
                    label = feed.optString("label", "Calendar"),
                    url = feed.optString("url"),
                )
              }
              .filter { it.url.isNotBlank() && !it.url.startsWith("YOUR_") && !it.url.contains("example.com") }
        }
        .getOrDefault(emptyList())

private fun parseIcsEvents(ics: String, sourceLabel: String): List<PortalEvent> {
  val zone = ZoneId.systemDefault()
  val now = ZonedDateTime.now(zone)
  val until = now.plusDays(14)
  val lines = unfoldIcsLines(ics)
  val parsedEvents = mutableListOf<Pair<ZonedDateTime, PortalEvent>>()
  var inEvent = false
  var summary = ""
  var start: ZonedDateTime? = null
  var location = ""

  lines.forEach { line ->
    when {
      line == "BEGIN:VEVENT" -> {
        inEvent = true
        summary = ""
        start = null
        location = ""
      }
      line == "END:VEVENT" -> {
        val eventStart = start
        if (inEvent && eventStart != null && !eventStart.isBefore(now.minusHours(1)) && eventStart.isBefore(until)) {
          parsedEvents += eventStart to
              PortalEvent(
                  time = formatEventTime(eventStart, now),
                  title = unescapeIcsText(summary).ifBlank { "Calendar event" },
                  detail = unescapeIcsText(location).ifBlank { sourceLabel },
              )
        }
        inEvent = false
      }
      inEvent && line.startsWith("SUMMARY") -> summary = line.valueAfterColon()
      inEvent && line.startsWith("LOCATION") -> location = line.valueAfterColon()
      inEvent && line.startsWith("DTSTART") -> start = parseIcsDate(line.valueAfterColon(), line, zone)
    }
  }

  return parsedEvents.sortedBy { it.first }.take(8).map { it.second }
}

private fun unfoldIcsLines(ics: String): List<String> {
  val result = mutableListOf<String>()
  ics.replace("\r\n", "\n").replace("\r", "\n").split("\n").forEach { rawLine ->
    if ((rawLine.startsWith(" ") || rawLine.startsWith("\t")) && result.isNotEmpty()) {
      result[result.lastIndex] = result.last() + rawLine.drop(1)
    } else {
      result += rawLine.trimEnd()
    }
  }
  return result
}

private fun parseIcsDate(value: String, fullLine: String, zone: ZoneId): ZonedDateTime? =
    runCatching {
          when {
            fullLine.contains("VALUE=DATE") ->
                LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE).atStartOfDay(zone)
            value.endsWith("Z") ->
                ZonedDateTime.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX"))
                    .withZoneSameInstant(zone)
            else ->
                LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")).atZone(zone)
          }
        }
        .getOrNull()

private fun formatEventTime(start: ZonedDateTime, now: ZonedDateTime): String {
  val day =
      when (start.toLocalDate()) {
        now.toLocalDate() -> "Today"
        now.plusDays(1).toLocalDate() -> "Tomorrow"
        else -> start.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US))
      }
  return "$day ${start.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))}"
}

private fun String.valueAfterColon(): String = substringAfter(':', "")

private fun unescapeIcsText(value: String): String =
    value
        .replace("\\n", " ")
        .replace("\\N", " ")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")

private fun syncPortalNotes(
    mode: PortalMode,
    notes: List<PortalNote>,
    onSuccess: (List<PortalNote>) -> Unit,
    onFailure: () -> Unit,
) {
  Thread {
        runCatching {
              val url = URL("$BACKEND_BASE_URL/api/portal/${mode.name.lowercase()}/notes")
              val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                connectTimeout = 1800
                readTimeout = 3000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
              }
              val payload = JSONObject().put("notes", notes.toJsonArray()).toString()
              connection.outputStream.use { output -> output.write(payload.toByteArray(Charsets.UTF_8)) }
              val responseCode = connection.responseCode
              if (responseCode !in 200..299) error("Sync failed with HTTP $responseCode")
              val response = connection.inputStream.bufferedReader().use { it.readText() }
              JSONObject(response).getJSONArray("notes").toPortalNotes()
            }
            .onSuccess { syncedNotes -> onSuccess(syncedNotes) }
            .onFailure { onFailure() }
      }
      .start()
}

private fun fetchAgentSummary(
    agent: String,
    mode: PortalMode,
    onSuccess: (String) -> Unit,
    onFailure: () -> Unit,
) {
  Thread {
        runCatching {
              val url = URL("$BACKEND_BASE_URL/api/agents/${agent.lowercase()}/summary?mode=${mode.name.lowercase()}")
              val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 1800
                readTimeout = 3000
              }
              val responseCode = connection.responseCode
              if (responseCode !in 200..299) error("Agent summary failed with HTTP $responseCode")
              val response = connection.inputStream.bufferedReader().use { it.readText() }
              JSONObject(response).optString("message", "No summary returned.")
            }
            .onSuccess(onSuccess)
            .onFailure { onFailure() }
      }
      .start()
}

private fun SyncStatus.color(): Color =
    when (this) {
      SyncStatus.Local -> Color(0xFFE6A23C)
      SyncStatus.Syncing -> Color(0xFF5B8CFF)
      SyncStatus.Synced -> Color(0xFF38B27A)
      SyncStatus.Offline -> Color(0xFFFF8A8A)
    }

private fun List<PortalNote>.toJsonArray(): JSONArray {
  val json = JSONArray()
  forEach { note -> json.put(note.toJson()) }
  return json
}

private fun JSONArray.toPortalNotes(): List<PortalNote> =
    List(length()) { index -> getJSONObject(index).toPortalNote() }
