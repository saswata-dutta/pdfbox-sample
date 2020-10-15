package pdfbox.sample;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceRGBColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceRGBColor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public class TextRunStripper extends PDFTextStripper {

  // page wise state
  private AffineTransform flipAT;
  private AffineTransform rotateAT;
  private AffineTransform transAT;

  private Map<TextPosition, RenderingMode> renderingMode;
  private Map<TextPosition, Integer> strokingColor;
  private Map<TextPosition, Integer> nonStrokingColor;
  private List<TextRun> textRuns;

  // document state
  private final List<List<TextRun>> pagedTextruns;

  public TextRunStripper() throws IOException {
    super();

    setSuppressDuplicateOverlappingText(false);
    addOperator(new SetStrokingColorSpace());
    addOperator(new SetNonStrokingColorSpace());
    addOperator(new SetStrokingDeviceCMYKColor());
    addOperator(new SetNonStrokingDeviceCMYKColor());
    addOperator(new SetNonStrokingDeviceRGBColor());
    addOperator(new SetStrokingDeviceRGBColor());
    addOperator(new SetNonStrokingDeviceGrayColor());
    addOperator(new SetStrokingDeviceGrayColor());
    addOperator(new SetStrokingColor());
    addOperator(new SetStrokingColorN());
    addOperator(new SetNonStrokingColor());
    addOperator(new SetNonStrokingColorN());

    pagedTextruns = new ArrayList<>();
  }

  public static List<List<TextRun>> processPdf(byte[] input) throws IOException {

    try (PDDocument document = PDDocument.load(input)) {
      TextRunStripper stripper = new TextRunStripper();
      stripper.setSortByPosition(true);

      for (int page = 0; page < document.getNumberOfPages(); ++page) {
        stripper.stripPage(document, page);
      }

      return stripper.getPagedTextruns();
    }
  }

  private void stripPage(PDDocument document, int pageNum) throws IOException {

    PDPage pdPage = document.getPage(pageNum);
    PDRectangle cropBox = pdPage.getCropBox();

    // flip y-axis
    flipAT = new AffineTransform();
    flipAT.translate(0, pdPage.getBBox().getHeight());
    flipAT.scale(1, -1);

    // page may be rotated
    rotateAT = new AffineTransform();
    int rotation = pdPage.getRotation();
    if (rotation != 0) {
      PDRectangle mediaBox = pdPage.getMediaBox();
      switch (rotation) {
        case 90:
          rotateAT.translate(mediaBox.getHeight(), 0);
          break;
        case 270:
          rotateAT.translate(0, mediaBox.getWidth());
          break;
        case 180:
          rotateAT.translate(mediaBox.getWidth(), mediaBox.getHeight());
          break;
        default:
          break;
      }
      rotateAT.rotate(Math.toRadians(rotation));
    }

    // cropbox
    transAT =
        AffineTransform.getTranslateInstance(-cropBox.getLowerLeftX(), cropBox.getLowerLeftY());

    // init state for page
    renderingMode = new HashMap<>();
    strokingColor = new HashMap<>();
    nonStrokingColor = new HashMap<>();
    textRuns = new ArrayList<>();

    setStartPage(pageNum + 1);
    setEndPage(pageNum + 1);

    Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
    writeText(document, dummy);

    pagedTextruns.add(textRuns);
  }

  @Override
  protected void processTextPosition(TextPosition text) {

    renderingMode.put(text, getGraphicsState().getTextState().getRenderingMode());
    try {
      strokingColor.put(text, getGraphicsState().getStrokingColor().toRGB());
      nonStrokingColor.put(text, getGraphicsState().getNonStrokingColor().toRGB());
    } catch (IOException e) {
      e.printStackTrace();
    }

    super.processTextPosition(text);
  }

  @Override
  protected void writeString(String string, List<TextPosition> textPositions) {
    List<Rectangle2D> bounds =
        textPositions.stream()
            .map(this::getBounds)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (bounds.size() > 0) {
      textRuns.add(
          new TextRun(
              string,
              union(bounds),
              strokingColor.get(textPositions.get(0)),
              nonStrokingColor.get(textPositions.get(0)),
              renderingMode.get(textPositions.get(0)),
              textPositions.get(0).getFontSize(),
              textPositions.get(0).getFont()));
    }
  }

  private static Rectangle2D union(List<Rectangle2D> bounds) {
    Rectangle2D result = bounds.get(0);
    for (int i = 1; i < bounds.size(); ++i) {
      result = result.createUnion(bounds.get(i));
    }
    return result;
  }

  private Rectangle2D getBounds(TextPosition text) {
    try {
      AffineTransform at = text.getTextMatrix().createAffineTransform();

      PDFont font = text.getFont();
      BoundingBox bbox = font.getBoundingBox();

      // advance width, bbox height (glyph space)
      float xadvance = font.getWidth(text.getCharacterCodes()[0]); // todo: should iterate all chars
      Rectangle2D.Float rect =
          new Rectangle2D.Float(0, bbox.getLowerLeftY(), xadvance, bbox.getHeight());

      if (font instanceof PDType3Font) {
        // bbox and font matrix are unscaled
        at.concatenate(font.getFontMatrix().createAffineTransform());
      } else {
        // bbox and font matrix are already scaled to 1000
        at.scale(1 / 1000f, 1 / 1000f);
      }
      Shape s = at.createTransformedShape(rect);
      s = flipAT.createTransformedShape(s);
      s = rotateAT.createTransformedShape(s);

      return s.getBounds2D();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public List<List<TextRun>> getPagedTextruns() {
    return pagedTextruns;
  }
}
