/*
 * The MIT License
 *
 * Copyright (c) 2013 Bruno P. Kinoshita
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.tap4j.plugin.util;

import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.ColorPalette;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.StackedAreaRenderer2;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.tap4j.plugin.AbstractTapProjectAction;
import org.tap4j.plugin.TapBuildAction;
import org.tap4j.plugin.TapResult;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for trend graph generation. 
 * 
 * @author TestNG plug-in  
 * @since 1.0
 */
public class GraphHelper
{

    /**
     * Do not instantiate GraphHelper.
     */
    private GraphHelper()
    {
        super();
    }

    public static void redirectWhenGraphUnsupported( StaplerResponse rsp,
            StaplerRequest req ) throws IOException
    {
        // not available. send out error message
        rsp.sendRedirect2(req.getContextPath() + "/images/headless.png");
    }

    public static JFreeChart createChart(StaplerRequest req, CategoryDataset dataset) {

      final JFreeChart chart = ChartFactory.createStackedAreaChart(
          "TAP Tests",                     // chart title
          null,                     // unused
          "TAP Tests Count",            // range axis label
          dataset,                  // data
          PlotOrientation.VERTICAL, // orientation
          true,                     // include legend
          true,                     // tooltips
          false                     // urls
      );

      // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
      final LegendTitle legend = chart.getLegend();
      legend.setPosition(RectangleEdge.RIGHT);

      chart.setBackgroundPaint(Color.white);

      final CategoryPlot plot = chart.getCategoryPlot();
      plot.setBackgroundPaint(Color.WHITE);
      plot.setOutlinePaint(null);
      plot.setForegroundAlpha(0.8f);
      plot.setDomainGridlinesVisible(true);
      plot.setDomainGridlinePaint(Color.white);
      plot.setRangeGridlinesVisible(true);
      plot.setRangeGridlinePaint(Color.black);

      CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
      plot.setDomainAxis(domainAxis);
      domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
      domainAxis.setLowerMargin(0.0);
      domainAxis.setUpperMargin(0.0);
      domainAxis.setCategoryMargin(0.0);

      final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
      rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

      StackedAreaRenderer ar = new StackedAreaRenderer2() {
        private static final long serialVersionUID = 331915263367089058L;

        @Override
        public String generateURL(CategoryDataset dataset, int row, int column) {
            NumberOnlyBuildLabel label = (NumberOnlyBuildLabel) dataset.getColumnKey(column);
            return  label.getRun().getNumber() + "/" + AbstractTapProjectAction.URL_NAME + "/";
        }

          @Override
        public String generateToolTip(CategoryDataset dataset, int row, int column) 
        {
              NumberOnlyBuildLabel label = (NumberOnlyBuildLabel) dataset.getColumnKey(column);
              TapBuildAction build = label.getRun().getAction(TapBuildAction.class);
              TapResult report = build.getResult();
              report.tally();

              switch (row) {
                  case 0:
                      return report.getFailed() + " Failure(s)";
                  case 1:
                     return report.getPassed() + " Pass";
                  case 2:
                     return report.getSkipped() + " Skip(s)";
                  case 3:
                     return report.getToDo() + " ToDo(s)";
                  default:
                     return "";
              }
          }

          @Override
          public boolean equals(Object obj) {
              return super.equals(obj);
          }
      };

      plot.setRenderer(ar);
      ar.setSeriesPaint(0, ColorPalette.RED); // Failures
      ar.setSeriesPaint(1, ColorPalette.BLUE); // Pass
      ar.setSeriesPaint(2, ColorPalette.YELLOW); // Skips
      ar.setSeriesPaint(3, Color.CYAN); // ToDo

      // crop extra space around the graph
      plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));

      return chart;
   }

    /**
     * Creates the graph displayed on Method results page to compare execution
     * duration and status of a test method across builds.
     * 
     * At max, 9 older builds are displayed.
     * 
     * @param req
     *            request
     * @param dataset
     *            data set to be displayed on the graph
     * @param statusMap
     *            a map with build as key and the test method's execution status
     *            (result) as the value
     * @param methodUrl
     *            URL to get to the method from a build test result page
     * @return the chart
     */
    public static JFreeChart createMethodChart( StaplerRequest req,
            final CategoryDataset dataset,
            final Map<NumberOnlyBuildLabel, String> statusMap,
            final String methodUrl )
    {

        final JFreeChart chart = ChartFactory.createBarChart(null, // chart
                                                                    // title
                null, // unused
                "� Duration (secs)",// range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // orientation
                true, // include legend
                true, // tooltips
                true // urls
                );

        // NOW DO SOME OPTIONAL CUSTOMISATION OF THE CHART...
        chart.setBackgroundPaint(Color.white);
        chart.removeLegend();

        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlinePaint(null);
        plot.setForegroundAlpha(0.8f);
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.black);

        CategoryAxis domainAxis = new ShiftedCategoryAxis(null);
        plot.setDomainAxis(domainAxis);
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        domainAxis.setLowerMargin(0.0);
        domainAxis.setUpperMargin(0.0);
        domainAxis.setCategoryMargin(0.0);

        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        BarRenderer br = new BarRenderer()
        {

            private static final long serialVersionUID = 961671076462240008L;
            final Map<String, Paint> statusPaintMap = new HashMap<>();

            {
                statusPaintMap.put("PASS", ColorPalette.BLUE);
                statusPaintMap.put("SKIP", ColorPalette.YELLOW);
                statusPaintMap.put("FAIL", ColorPalette.RED);
                statusPaintMap.put("TODO", ColorPalette.GREY);
            }

            /**
             * Returns the paint for an item. Overrides the default behaviour
             * inherited from AbstractSeriesRenderer.
             * 
             * @param row
             *            the series.
             * @param column
             *            the category.
             * 
             * @return The item colour.
             */
            public Paint getItemPaint( final int row, final int column )
            {
                NumberOnlyBuildLabel label = (NumberOnlyBuildLabel) dataset
                        .getColumnKey(column);
                Paint paint = statusPaintMap.get(statusMap.get(label));
                // when the status of test method is unknown, use grey colour
                return paint == null ? Color.gray : paint;
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj);
            }

            @Override
            public int hashCode() {
                return super.hashCode();
            }
        };

        br.setBaseToolTipGenerator((dataset1, row, column) -> {
            NumberOnlyBuildLabel label = (NumberOnlyBuildLabel) dataset1
                    .getColumnKey(column);
            if ("UNKNOWN".equals(statusMap.get(label)))
            {
                return "unknown";
            }
            // values are in seconds
            return dataset1.getValue(row, column) + " secs";
        });

        br.setBaseItemURLGenerator((dataset12, series, category) -> {
            NumberOnlyBuildLabel label = (NumberOnlyBuildLabel) dataset12
                    .getColumnKey(category);
            if ("UNKNOWN".equals(statusMap.get(label)))
            {
                // no link when method result doesn't exist
                return null;
            }
            // return label.build.getUpUrl() + label.build.getNumber() + "/" + PluginImpl.URL + "/" + methodUrl;
            return label.build.getUpUrl() + label.getRun().getNumber() + "/tap/" + methodUrl;
        });

        br.setItemMargin(0.0);
        br.setMinimumBarLength(5);
        // set the base to be 1/100th of the maximum value displayed in the
        // graph
        br.setBase(br.findRangeBounds(dataset).getUpperBound() / 100);
        plot.setRenderer(br);

        // crop extra space around the graph
        plot.setInsets(new RectangleInsets(0, 0, 0, 5.0));
        return chart;
    }

}
