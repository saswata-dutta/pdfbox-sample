package org.saswata;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.saswata.pdf.TextRun;

public class AwsBillParser {
  public static List<AwsBillLineItem> parseLineItems(List<List<TextRun>> pagedTextRuns) {
    List<AwsBillLineItem> allLineItems = new ArrayList<>();
    for (List<TextRun> textRuns : pagedTextRuns) {
      List<TextRun> candidates = guessLineItems(textRuns);
      List<TextRun[]> items = filterLineItems(textRuns, candidates);
      items.forEach(it -> allLineItems.add(new AwsBillLineItem(it[0].text, getAmount(it[1].text))));
    }
    return allLineItems;
  }

  private static List<TextRun> guessLineItems(List<TextRun> textRuns) {
    return textRuns.stream()
        .filter(
            t ->
                t.fontSize == 10
                    && t.nonStrokingColor == 16750848
                    && RenderingMode.FILL == t.renderingMode)
        .collect(Collectors.toList());
  }

  private static List<TextRun[]> filterLineItems(List<TextRun> textRuns, List<TextRun> candidates) {
    List<TextRun[]> lineItems = new ArrayList<>();
    for (TextRun candidate : candidates) {
      List<TextRun> amounts = guessAmount(textRuns, candidate);
      if (amounts.size() == 1) {
        lineItems.add(new TextRun[] {candidate, amounts.get(0)});
      }
    }

    return lineItems;
  }

  private static List<TextRun> guessAmount(List<TextRun> textRuns, TextRun candidate) {
    return textRuns.stream()
        .filter(
            t ->
                t != candidate
                    && t.text.charAt(0) == '$'
                    && hasVerticalOverlap(candidate.box, t.box))
        .collect(Collectors.toList());
  }

  private static boolean hasVerticalOverlap(Rectangle2D lhs, Rectangle2D rhs) {
    double center = lhs.getCenterY();
    double top = rhs.getY() + rhs.getHeight();
    double bottom = rhs.getY();
    return bottom <= center && center <= top;
  }

  private static int getAmount(String amount) {
    return Float.valueOf(amount.replaceAll("\\$", "").replaceAll(",", "")).intValue();
  }
}
