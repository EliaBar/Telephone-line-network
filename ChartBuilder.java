import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.ImageIO;

public class ChartBuilder {
    private int width;
    private int height;
    private Color backgroundColor;
    private Color axisColor;
    private Color gridColor;
    private Color barFillColor;
    private Color barBorderColor;
    private Color[] seriesColors;
    private Font titleFont;
    private Font labelFont;
    private Font axisTitleFont;

    public ChartBuilder() {
        this.width = 1150;
        this.height = 480;
        this.backgroundColor = Color.WHITE;
        this.axisColor = Color.BLACK;
        this.gridColor = new Color(225, 225, 225);
        this.barFillColor = new Color(70, 130, 180);
        this.barBorderColor = new Color(25, 25, 112);
        this.seriesColors = new Color[]{
            new Color(31, 119, 180),
            new Color(255, 127, 14),
            new Color(44, 160, 44),
            new Color(214, 39, 40)
        };
        this.titleFont = new Font("SansSerif", Font.BOLD, 13);
        this.labelFont = new Font("SansSerif", Font.PLAIN, 11);
        this.axisTitleFont = new Font("SansSerif", Font.BOLD, 12);
    }

    public ChartBuilder setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public void buildTransientChart(String fileName, double[][] histP, double[][] histT, double step,
                                    double t1, double t2, double lowP, double highP, double lowT, double highT) {
        BufferedImage img = createCanvas();
        Graphics2D g = initGraphics(img);

        int plotW = (width - 230) / 2;
        int plotH = height - 110;
        drawConvergenceSubPlot(g, histP, step, t1, lowP, highP, 65, 45, plotW, plotH, "P_retry", "P_retry");
        drawConvergenceSubPlot(g, histT, step, t2, lowT, highT, 155 + plotW, 45, plotW, plotH, "T_total", "T_total, min");

        g.dispose();
        saveImage(img, fileName, "Transient period chart saved to file: " + fileName);
    }

    public void buildHistograms(String fileName, String[] titles, String[] xLabels, double[][] allSamples, int binsCount) {
        int cols = 3;
        int rows = 2;
        int cellWidth = 540;
        int cellHeight = 440;
        int width = cols * cellWidth;
        int height = rows * cellHeight;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        int marginLeft = 65;
        int marginRight = 30;
        int marginTop = 55;
        int marginBottom = 55;

        for (int k = 0; k < allSamples.length; k++) {
            int row = k / cols;
            int col = k % cols;
            int baseX = col * cellWidth;
            int baseY = row * cellHeight;

            int plotX = baseX + marginLeft;
            int plotY = baseY + marginTop;
            int plotW = cellWidth - marginLeft - marginRight;
            int plotH = cellHeight - marginTop - marginBottom;

            drawSingleHistogram(g, allSamples[k], binsCount, titles[k], xLabels[k], plotX, plotY, plotW, plotH);
        }

        g.dispose();
        try {
            ImageIO.write(image, "png", new File(fileName));
            System.out.println("Histograms chart saved to file: " + fileName);
        } catch (IOException e) {
            System.err.println("Failed to save histograms chart: " + e.getMessage());
        }
    }

    private void drawSingleHistogram(Graphics2D g, double[] data, int binsCount,
                                     String title, String xLabel,
                                     int plotX, int plotY, int plotW, int plotH) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double sum = 0.0;
        for (double v : data) {
            if (v < min) {
                min = v;
            }
            if (v > max) {
                max = v;
            }
            sum += v;
        }
        double mean = sum / data.length;

        double span = max - min;
        if (span == 0.0) {
            span = 1.0;
        }
        double binWidth = span / binsCount;

        int[] counts = new int[binsCount];
        int maxCount = 0;
        for (double v : data) {
            int idx = (int) ((v - min) / binWidth);
            if (idx < 0) {
                idx = 0;
            }
            if (idx >= binsCount) {
                idx = binsCount - 1;
            }
            counts[idx]++;
            if (counts[idx] > maxCount) {
                maxCount = counts[idx];
            }
        }
        if (maxCount == 0) {
            maxCount = 1;
        }

        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 15));
        g.drawString(title, plotX + plotW / 2 - g.getFontMetrics().stringWidth(title) / 2, plotY - 20);

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.drawString("Freq", plotX - 45, plotY + 12);
        g.drawString(xLabel, plotX + plotW - g.getFontMetrics().stringWidth(xLabel), plotY + plotH + 40);

        double barPixelWidth = (double) plotW / binsCount;
        for (int i = 0; i < binsCount; i++) {
            int barH = (int) Math.round(((double) counts[i] / maxCount) * (plotH - 10));
            int bx = plotX + (int) Math.round(i * barPixelWidth);
            int bw = Math.max(1, (int) Math.round((i + 1) * barPixelWidth) - (int) Math.round(i * barPixelWidth));
            int by = plotY + plotH - barH;

            g.setColor(new Color(70, 130, 180));
            g.fillRect(bx, by, bw, barH);
            g.setColor(new Color(35, 65, 95));
            g.drawRect(bx, by, bw, barH);
        }

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(plotX, plotY, plotX, plotY + plotH);
        g.drawLine(plotX, plotY + plotH, plotX + plotW, plotY + plotH);

        int meanX = plotX + (int) Math.round(((mean - min) / span) * plotW);
        Stroke oldStroke = g.getStroke();
        g.setColor(new Color(200, 40, 40));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        g.drawLine(meanX, plotY, meanX, plotY + plotH);
        g.setStroke(oldStroke);

        g.setColor(Color.BLACK);
        g.drawString(String.valueOf(maxCount), plotX - 35, plotY + 25);
        g.drawString("0", plotX - 18, plotY + plotH);
        g.drawString(String.format(Locale.US, "%.3f", min), plotX, plotY + plotH + 18);
        String maxStr = String.format(Locale.US, "%.3f", max);
        g.drawString(maxStr, plotX + plotW - g.getFontMetrics().stringWidth(maxStr), plotY + plotH + 18);
    }

    private BufferedImage createCanvas() {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    private Graphics2D initGraphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(backgroundColor);
        g.fillRect(0, 0, width, height);
        return g;
    }

    private void saveImage(BufferedImage img, String fileName, String successMessage) {
        try {
            ImageIO.write(img, "png", new File(fileName));
            System.out.println(successMessage);
        } catch (Exception e) {
            System.err.println("Failed to save chart (" + fileName + "): " + e.getMessage());
        }
    }

    private void drawConvergenceSubPlot(Graphics2D g, double[][] data, double step, double tMark,
                                        double lowBound, double highBound,
                                        int x0, int y0, int w, int h, String title, String yLabel) {
        int runs = data.length;
        int steps = data[0].length;
        double maxTime = steps * step;

        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int r = 0; r < runs; r++) {
            for (int s = 0; s < steps; s++) {
                if (data[r][s] < minY) {
                    minY = data[r][s];
                }
                if (data[r][s] > maxY) {
                    maxY = data[r][s];
                }
            }
        }
        double pad = (maxY - minY) * 0.1 + 1e-6;
        minY = Math.max(0, minY - pad);
        maxY += pad;

        g.setColor(axisColor);
        g.setFont(titleFont);
        g.drawString(title, x0 + 55, y0 - 15);
        g.drawLine(x0, y0, x0, y0 + h);
        g.drawLine(x0, y0 + h, x0 + w, y0 + h);

        g.setFont(labelFont);
        g.drawString(yLabel, x0 - 50, y0 + 10);
        g.drawString("Time t", x0 + w - 30, y0 + h + 30);
        g.drawString(String.format(Locale.US, "%.2f", maxY), x0 - 52, y0 + 5);
        g.drawString(String.format(Locale.US, "%.2f", minY), x0 - 52, y0 + h);
        g.drawString("0", x0 - 5, y0 + h + 15);
        g.drawString(String.format(Locale.US, "%.0f", maxTime), x0 + w - 25, y0 + h + 15);

        Stroke oldStroke = g.getStroke();
        g.setColor(new Color(120, 120, 120));
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4}, 0));
        int yLow = y0 + h - (int) (((Math.max(minY, lowBound) - minY) / (maxY - minY)) * h);
        int yHigh = y0 + h - (int) (((Math.min(maxY, highBound) - minY) / (maxY - minY)) * h);
        g.drawLine(x0, yLow, x0 + w, yLow);
        g.drawLine(x0, yHigh, x0 + w, yHigh);

        int xT = x0 + (int) ((tMark / maxTime) * w);
        g.setColor(Color.DARK_GRAY);
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
        g.drawLine(xT, y0, xT, y0 + h);
        g.setStroke(oldStroke);
        g.drawString(String.format(Locale.US, "T = %.0f", tMark), xT + 4, y0 + 18);

        for (int r = 0; r < runs; r++) {
            g.setColor(seriesColors[r % seriesColors.length]);
            g.setStroke(new BasicStroke(1.8f));
            for (int s = 0; s < steps - 1; s++) {
                int x1 = x0 + (int) (((s + 1) * step / maxTime) * w);
                int y1 = y0 + h - (int) (((data[r][s] - minY) / (maxY - minY)) * h);
                int x2 = x0 + (int) (((s + 2) * step / maxTime) * w);
                int y2 = y0 + h - (int) (((data[r][s + 1] - minY) / (maxY - minY)) * h);
                g.drawLine(x1, y1, x2, y2);
            }
        }
    }

    public void buildFactorialChart(String fileName,
                                    String name1, double[] mean1, double[] var1,
                                    String name2, double[] mean2, double[] var2) {
        BufferedImage img = createCanvas();
        Graphics2D g = initGraphics(img);

        String[] labels = {"Exp 1", "Exp 2", "Exp 3", "Exp 4", "Exp 5", "Exp 6", "Exp 7", "Exp 8"};
        int plotW = (width - 230) / 2;
        int plotH = height - 110;

        drawProfileSubPlot(g, labels, mean1, varToStd(var1), 65, 45, plotW, plotH,
                "Factorial 2^3: " + name1 + " (mean +/- SD)", name1, seriesColors[0]);
        drawProfileSubPlot(g, labels, mean2, varToStd(var2), 155 + plotW, 45, plotW, plotH,
                "Factorial 2^3: " + name2 + " (mean +/- SD)", name2, seriesColors[3]);

        g.dispose();
        saveImage(img, fileName, "Factorial chart saved to file: " + fileName);
    }

    public void buildAnovaChart(String fileName,
                                int[] l1Levels, double[] l1MeanP, double[] l1StdP, double[] l1MeanT, double[] l1StdT,
                                int[] l2Levels, double[] l2MeanP, double[] l2StdP, double[] l2MeanT, double[] l2StdT) {
        int cols = 2;
        int rows = 2;
        int cellW = 580;
        int cellH = 420;
        int totalW = cols * cellW;
        int totalH = rows * cellH;

        BufferedImage img = new BufferedImage(totalW, totalH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(backgroundColor);
        g.fillRect(0, 0, totalW, totalH);

        String[] l1Labels = new String[l1Levels.length];
        for (int i = 0; i < l1Levels.length; i++) {
            l1Labels[i] = "L1 = " + l1Levels[i];
        }
        String[] l2Labels = new String[l2Levels.length];
        for (int i = 0; i < l2Levels.length; i++) {
            l2Labels[i] = "L2 = " + l2Levels[i];
        }

        int mLeft = 75;
        int mRight = 35;
        int mTop = 50;
        int mBottom = 55;
        int pW = cellW - mLeft - mRight;
        int pH = cellH - mTop - mBottom;

        drawProfileSubPlot(g, l1Labels, l1MeanP, l1StdP, mLeft, mTop, pW, pH,
                "ANOVA: External lines L1 -> P_retry", "P_retry", seriesColors[0]);
        drawProfileSubPlot(g, l1Labels, l1MeanT, l1StdT, cellW + mLeft, mTop, pW, pH,
                "ANOVA: External lines L1 -> T_total", "T_total, min", seriesColors[3]);
        drawProfileSubPlot(g, l2Labels, l2MeanP, l2StdP, mLeft, cellH + mTop, pW, pH,
                "ANOVA: Internal lines L2 -> P_retry", "P_retry", seriesColors[2]);
        drawProfileSubPlot(g, l2Labels, l2MeanT, l2StdT, cellW + mLeft, cellH + mTop, pW, pH,
                "ANOVA: Internal lines L2 -> T_total", "T_total, min", seriesColors[1]);

        g.dispose();
        saveImage(img, fileName, "ANOVA chart saved to file: " + fileName);
    }

    private double[] varToStd(double[] variances) {
        double[] stds = new double[variances.length];
        for (int i = 0; i < variances.length; i++) {
            stds[i] = Math.sqrt(Math.max(0.0, variances[i]));
        }
        return stds;
    }

    private void drawProfileSubPlot(Graphics2D g, String[] xLabels, double[] means, double[] stds,
                                    int x0, int y0, int w, int h, String title, String yLabel, Color lineColor) {
        int n = means.length;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (means[i] - stds[i] < minY) {
                minY = means[i] - stds[i];
            }
            if (means[i] + stds[i] > maxY) {
                maxY = means[i] + stds[i];
            }
        }
        double pad = (maxY - minY) * 0.15 + 1e-6;
        minY = Math.max(0.0, minY - pad);
        maxY += pad;

        g.setColor(axisColor);
        g.setFont(titleFont);
        g.drawString(title, x0 + w / 2 - g.getFontMetrics().stringWidth(title) / 2, y0 - 16);

        g.setFont(labelFont);
        int yDivs = 5;
        for (int i = 0; i <= yDivs; i++) {
            int y = y0 + h - (i * h / yDivs);
            g.setColor(gridColor);
            g.drawLine(x0, y, x0 + w, y);
            g.setColor(axisColor);
            g.drawLine(x0 - 4, y, x0, y);
            double val = minY + ((maxY - minY) / yDivs) * i;
            String lbl = String.format(Locale.US, "%.3f", val);
            g.drawString(lbl, x0 - g.getFontMetrics().stringWidth(lbl) - 8, y + 4);
        }

        g.setColor(axisColor);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(x0, y0, x0, y0 + h);
        g.drawLine(x0, y0 + h, x0 + w, y0 + h);
        g.drawString(yLabel, x0 - 55, y0 - 15);

        int[] px = new int[n];
        int[] py = new int[n];
        for (int i = 0; i < n; i++) {
            px[i] = x0 + (int) Math.round(((i + 0.5) / n) * w);
            py[i] = y0 + h - (int) Math.round(((means[i] - minY) / (maxY - minY)) * h);

            int yTop = y0 + h - (int) Math.round(((Math.min(maxY, means[i] + stds[i]) - minY) / (maxY - minY)) * h);
            int yBot = y0 + h - (int) Math.round(((Math.max(minY, means[i] - stds[i]) - minY) / (maxY - minY)) * h);

            g.setColor(Color.DARK_GRAY);
            g.setStroke(new BasicStroke(1.3f));
            g.drawLine(px[i], yTop, px[i], yBot);
            g.drawLine(px[i] - 5, yTop, px[i] + 5, yTop);
            g.drawLine(px[i] - 5, yBot, px[i] + 5, yBot);

            g.setColor(axisColor);
            int lblW = g.getFontMetrics().stringWidth(xLabels[i]);
            g.drawString(xLabels[i], px[i] - lblW / 2, y0 + h + 18);
        }

        g.setColor(lineColor);
        g.setStroke(new BasicStroke(2.2f));
        for (int i = 0; i < n - 1; i++) {
            g.drawLine(px[i], py[i], px[i + 1], py[i + 1]);
        }

        for (int i = 0; i < n; i++) {
            g.setColor(lineColor);
            g.fillOval(px[i] - 4, py[i] - 4, 8, 8);
            g.setColor(Color.BLACK);
            g.drawOval(px[i] - 4, py[i] - 4, 8, 8);
        }
    }
}