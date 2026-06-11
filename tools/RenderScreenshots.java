import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public final class RenderScreenshots {
  private static final int WIDTH = 800;
  private static final int HEIGHT = 1280;
  private static final Color BG = new Color(23, 26, 28);
  private static final Color SURFACE = new Color(36, 41, 45);
  private static final Color SURFACE_2 = new Color(48, 53, 56);
  private static final Color TEXT = new Color(245, 242, 235);
  private static final Color MUTED = new Color(190, 185, 176);
  private static final Color BLUE = new Color(63, 143, 127);
  private static final Color GREEN = new Color(95, 175, 130);
  private static final Color AMBER = new Color(214, 166, 79);
  private static final Color ROSE = new Color(217, 129, 149);

  public static void main(String[] args) throws Exception {
    File out = new File("docs/screenshots");
    out.mkdirs();
    write("portal-hub-home.png", drawHome());
    write("portal-hub-lists.png", drawLists());
    write("portal-hub-note-editor.png", drawEditor());
  }

  private static BufferedImage drawHome() {
    BufferedImage image = canvas();
    Graphics2D g = image.createGraphics();
    setup(g);
    shell(g, "Portal Hub", "Home portal", "Local", AMBER);
    tabs(g, true);
    sectionTabs(g, "Calendar");
    section(g, "Schedule", 48, 304);
    calendar(g, 48, 340, "Calendar", "Add calendar feed", "Apple or Google read-only feed");
    calendar(g, 48, 468, "Sync", "Tap Sync after install", "Rebuild after editing local feed file");
    summary(g, 48, 632, "Events", "2", "visible", AMBER);
    summary(g, 284, 632, "Open", "3", "notes", GREEN);
    summary(g, 520, 632, "Pinned", "1", "important", ROSE);
    section(g, "Today", 48, 800);
    focus(g, 48, 836, "Kids", "Library books and class bag need a quick check.", ROSE);
    focus(g, 48, 950, "Groceries", "Milk, fruit, yogurt, and lunchbox snacks.", GREEN);
    g.dispose();
    return image;
  }

  private static BufferedImage drawLists() {
    BufferedImage image = canvas();
    Graphics2D g = image.createGraphics();
    setup(g);
    shell(g, "Portal Hub", "Home portal", "Synced", GREEN);
    tabs(g, true);
    sectionTabs(g, "Lists");
    section(g, "Lists", 48, 304);
    checklist(g, 48, 340, "Grocery", GREEN, new String[] {"Milk", "Strawberries", "Yogurt", "Lunchbox snacks"});
    checklist(g, 48, 604, "School & Kids", ROSE, new String[] {"Blue folder", "Library book", "Class bag"});
    checklist(g, 48, 824, "Other", AMBER, new String[] {"Gift ideas", "Packing list"});
    g.dispose();
    return image;
  }

  private static BufferedImage drawEditor() {
    BufferedImage image = canvas();
    Graphics2D g = image.createGraphics();
    setup(g);
    shell(g, "Portal Hub", "Home portal", "Local", AMBER);
    tabs(g, true);
    round(g, 56, 250, 688, 730, 24, SURFACE);
    text(g, "Edit Note", 96, 318, 34, Font.BOLD, TEXT);
    round(g, 96, 370, 608, 480, 18, new Color(25, 30, 40));
    text(g, "Weekend ideas", 126, 430, 28, Font.BOLD, TEXT);
    text(g, "Look up summer camps, save recipe links,", 126, 486, 24, Font.PLAIN, TEXT);
    text(g, "and jot down birthday gift ideas here.", 126, 526, 24, Font.PLAIN, TEXT);
    text(g, "This is just a freeform note.", 126, 604, 24, Font.PLAIN, MUTED);
    button(g, 96, 1038, 180, 62, "Archive", SURFACE_2, TEXT);
    button(g, 296, 1038, 160, 62, "Delete", new Color(74, 39, 48), new Color(255, 138, 138));
    button(g, 476, 1038, 228, 62, "Update", BLUE, Color.WHITE);
    g.dispose();
    return image;
  }

  private static BufferedImage canvas() {
    BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    setup(g);
    g.setColor(BG);
    g.fillRect(0, 0, WIDTH, HEIGHT);
    g.dispose();
    return image;
  }

  private static void write(String name, BufferedImage image) throws Exception {
    ImageIO.write(image, "png", new File("docs/screenshots/" + name));
  }

  private static void setup(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  }

  private static void shell(Graphics2D g, String title, String subtitle, String status, Color statusColor) {
    round(g, 0, 0, WIDTH, 138, 0, new Color(34, 61, 56));
    text(g, title, 48, 58, 32, Font.BOLD, TEXT);
    text(g, subtitle, 48, 98, 21, Font.PLAIN, MUTED);
    round(g, 604, 46, 148, 50, 25, new Color(23, 27, 36));
    dot(g, 630, 71, 8, statusColor);
    text(g, status, 654, 80, 19, Font.BOLD, TEXT);
  }

  private static void tabs(Graphics2D g, boolean home) {
    button(g, 48, 156, 340, 64, "Home", home ? BLUE : SURFACE_2, TEXT);
    button(g, 412, 156, 340, 64, "Office", home ? SURFACE_2 : BLUE, TEXT);
  }

  private static void hero(Graphics2D g, String title, String subtitle) {
    round(g, 48, 240, 704, 112, 18, SURFACE);
    text(g, title, 82, 288, 31, Font.BOLD, TEXT);
    text(g, subtitle, 82, 324, 21, Font.PLAIN, MUTED);
  }

  private static void focus(Graphics2D g, int x, int y, String title, String detail, Color color) {
    round(g, x, y, 704, 88, 16, SURFACE);
    dot(g, x + 34, y + 44, 11, color);
    text(g, title, x + 62, y + 36, 24, Font.BOLD, TEXT);
    text(g, detail, x + 62, y + 66, 19, Font.PLAIN, MUTED);
  }

  private static void note(Graphics2D g, int x, int y, String title, String person, String category, String due, boolean pinned) {
    round(g, x, y, 704, 104, 16, SURFACE);
    text(g, title, x + 28, y + 42, 22, Font.BOLD, TEXT);
    text(g, person + "  /  " + category + "  /  " + due, x + 28, y + 78, 18, Font.PLAIN, MUTED);
    if (pinned) {
      button(g, x + 586, y + 28, 82, 42, "Pinned", new Color(63, 53, 32), new Color(255, 213, 126));
    }
  }

  private static void sectionTabs(Graphics2D g, String selected) {
    String[] labels = {"Calendar", "Lists", "Chores", "Notes"};
    int cursor = 48;
    for (String label : labels) {
      int width = label.length() * 15 + 36;
      button(g, cursor, 228, width, 50, label, label.equals(selected) ? BLUE : SURFACE_2, TEXT);
      cursor += width + 10;
    }
  }

  private static void summary(Graphics2D g, int x, int y, String label, String value, String detail, Color color) {
    round(g, x, y, 216, 104, 16, SURFACE);
    dot(g, x + 28, y + 28, 7, color);
    text(g, label, x + 44, y + 34, 17, Font.BOLD, MUTED);
    text(g, value, x + 22, y + 72, 30, Font.BOLD, TEXT);
    text(g, detail, x + 22, y + 94, 16, Font.PLAIN, MUTED);
  }

  private static void calendar(Graphics2D g, int x, int y, String time, String title, String detail) {
    round(g, x, y, 704, 104, 16, SURFACE);
    text(g, time, x + 28, y + 34, 19, Font.BOLD, AMBER);
    text(g, title, x + 28, y + 64, 24, Font.BOLD, TEXT);
    text(g, detail, x + 28, y + 92, 18, Font.PLAIN, MUTED);
  }

  private static void checklist(Graphics2D g, int x, int y, String title, Color color, String[] items) {
    int h = 74 + items.length * 44;
    round(g, x, y, 704, h, 16, SURFACE);
    dot(g, x + 32, y + 34, 8, color);
    text(g, title, x + 52, y + 42, 24, Font.BOLD, TEXT);
    int rowY = y + 82;
    for (int i = 0; i < items.length; i++) {
      String item = items[i];
      boolean checked = i == 0 && ("Grocery".equals(title) || "Household".equals(title));
      g.setColor(MUTED);
      g.setStroke(new BasicStroke(3));
      g.drawRoundRect(x + 28, rowY - 20, 24, 24, 6, 6);
      if (checked) {
        g.setColor(BLUE);
        g.drawLine(x + 33, rowY - 7, x + 39, rowY - 1);
        g.drawLine(x + 39, rowY - 1, x + 49, rowY - 15);
      }
      text(g, item, x + 68, rowY, 20, Font.BOLD, TEXT);
      if (checked) {
        g.setColor(MUTED);
        g.setStroke(new BasicStroke(2));
        g.drawLine(x + 68, rowY - 7, x + 68 + item.length() * 13, rowY - 7);
      }
      rowY += 44;
    }
  }

  private static void chips(Graphics2D g, int x, int y, String[] labels, int selected) {
    int cursor = x;
    for (int i = 0; i < labels.length; i++) {
      int width = Math.max(90, labels[i].length() * 15 + 34);
      button(g, cursor, y, width, 50, labels[i], i == selected ? BLUE : SURFACE_2, TEXT);
      cursor += width + 12;
      if (cursor > 640) {
        cursor = x;
        y += 62;
      }
    }
  }

  private static void section(Graphics2D g, String title, int x, int y) {
    text(g, title, x, y, 28, Font.BOLD, TEXT);
  }

  private static void label(Graphics2D g, String value, int x, int y) {
    text(g, value, x, y, 20, Font.BOLD, MUTED);
  }

  private static void button(Graphics2D g, int x, int y, int w, int h, String label, Color bg, Color fg) {
    round(g, x, y, w, h, 18, bg);
    g.setColor(fg);
    g.setFont(new Font("SansSerif", Font.BOLD, 18));
    FontMetrics fm = g.getFontMetrics();
    g.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
  }

  private static void round(Graphics2D g, int x, int y, int w, int h, int r, Color color) {
    g.setColor(color);
    if (r <= 0) {
      g.fillRect(x, y, w, h);
    } else {
      g.fill(new RoundRectangle2D.Double(x, y, w, h, r, r));
    }
  }

  private static void dot(Graphics2D g, int x, int y, int r, Color color) {
    g.setColor(color);
    g.fillOval(x - r, y - r, r * 2, r * 2);
  }

  private static void text(Graphics2D g, String value, int x, int y, int size, int style, Color color) {
    g.setColor(color);
    g.setFont(new Font("SansSerif", style, size));
    g.drawString(value, x, y);
  }
}
