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
  private static final Color BG = new Color(18, 21, 28);
  private static final Color SURFACE = new Color(30, 35, 46);
  private static final Color SURFACE_2 = new Color(39, 46, 60);
  private static final Color TEXT = new Color(244, 247, 251);
  private static final Color MUTED = new Color(171, 181, 196);
  private static final Color BLUE = new Color(91, 140, 255);
  private static final Color GREEN = new Color(56, 178, 122);
  private static final Color AMBER = new Color(230, 162, 60);
  private static final Color ROSE = new Color(230, 106, 138);

  public static void main(String[] args) throws Exception {
    File out = new File("docs/screenshots");
    out.mkdirs();
    write("portal-hub-home.png", drawHome());
    write("portal-hub-office.png", drawOffice());
    write("portal-hub-note-editor.png", drawEditor());
  }

  private static BufferedImage drawHome() {
    BufferedImage image = canvas();
    Graphics2D g = image.createGraphics();
    setup(g);
    shell(g, "Portal Hub", "Home portal", "Local", AMBER);
    tabs(g, true);
    hero(g, "Family reset", "Check kids reminders, groceries, and open household notes.");
    focus(g, 48, 380, "Kids", "Library books and class bag need a quick check.", ROSE);
    focus(g, 48, 494, "Groceries", "Milk, fruit, yogurt, and lunchbox snacks.", GREEN);
    focus(g, 48, 608, "Household", "Trash tonight. Home cleanup after dinner.", AMBER);
    section(g, "Open Notes", 48, 760);
    note(g, 48, 818, "Bring blue folder tomorrow", "Child 1", "School", "Tomorrow", true);
    note(g, 48, 946, "Buy strawberries and yogurt", "You", "Grocery", "Today", false);
    note(g, 48, 1074, "Prep dinner before lunch", "Helper", "Food", "Today", false);
    g.dispose();
    return image;
  }

  private static BufferedImage drawOffice() {
    BufferedImage image = canvas();
    Graphics2D g = image.createGraphics();
    setup(g);
    shell(g, "Portal Hub", "Office portal", "Synced", GREEN);
    tabs(g, false);
    hero(g, "Focus block", "Review target roles, move one application, capture wins.");
    focus(g, 48, 380, "Career", "Pick one role and write the positioning angle.", BLUE);
    focus(g, 48, 494, "Interview", "Draft one story using STAR and impact notes.", ROSE);
    focus(g, 48, 608, "Learning", "Log the next local-model experiment.", GREEN);
    section(g, "Open Notes", 48, 760);
    note(g, 48, 818, "Shape one side-project interview story", "You", "Career", "This week", true);
    note(g, 48, 946, "Draft a short post from this project", "Writer", "Writing", "Friday", false);
    note(g, 48, 1074, "Follow up on target company shortlist", "Execution", "Applications", "Tomorrow", false);
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
    round(g, 96, 370, 608, 142, 18, new Color(25, 30, 40));
    text(g, "Buy strawberries and yogurt", 126, 448, 28, Font.PLAIN, TEXT);
    label(g, "Person", 96, 570);
    chips(g, 96, 602, new String[] {"You", "Partner", "Child 1", "Child 2", "Helper"}, 0);
    label(g, "Category", 96, 760);
    chips(g, 96, 792, new String[] {"Grocery", "School", "Food", "Kids"}, 0);
    label(g, "Due", 96, 900);
    chips(g, 96, 932, new String[] {"Today", "Tomorrow", "This week"}, 0);
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
    round(g, 0, 0, WIDTH, 138, 0, new Color(41, 51, 76));
    text(g, title, 48, 58, 32, Font.BOLD, TEXT);
    text(g, subtitle, 48, 98, 21, Font.PLAIN, MUTED);
    round(g, 604, 46, 148, 50, 25, new Color(23, 27, 36));
    dot(g, 630, 71, 8, statusColor);
    text(g, status, 654, 80, 19, Font.BOLD, TEXT);
  }

  private static void tabs(Graphics2D g, boolean home) {
    button(g, 48, 156, 148, 54, "Home", home ? BLUE : SURFACE_2, TEXT);
    button(g, 212, 156, 156, 54, "Office", home ? SURFACE_2 : BLUE, TEXT);
    button(g, 572, 156, 180, 54, "+ Add note", GREEN, Color.WHITE);
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
