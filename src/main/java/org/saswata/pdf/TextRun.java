package org.saswata.pdf;

import java.awt.geom.Rectangle2D;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;

public class TextRun {
  public final String text;
  public final Rectangle2D box;
  public final int strokingColor;
  public final int nonStrokingColor;
  public final RenderingMode renderingMode;
  public final float fontSize;
  public final PDFont font;

  public TextRun(
      String text,
      Rectangle2D box,
      int strokingColor,
      int nonStrokingColor,
      RenderingMode renderingMode,
      float fontSize,
      PDFont font) {
    this.text = text;
    this.box = box;
    this.strokingColor = strokingColor;
    this.nonStrokingColor = nonStrokingColor;
    this.renderingMode = renderingMode;
    this.fontSize = fontSize;
    this.font = font;
  }

  @Override
  public String toString() {
    return "TextRun{"
        + "text='"
        + text
        + '\''
        + ", box="
        + box
        + ", strokingColor="
        + strokingColor
        + ", nonStrokingColor="
        + nonStrokingColor
        + ", renderingMode="
        + renderingMode
        + ", fontSize="
        + fontSize
        + ", font="
        + font
        + '}';
  }
}
