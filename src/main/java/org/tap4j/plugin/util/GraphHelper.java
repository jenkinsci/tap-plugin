package org.tap4j.plugin.util;

import hudson.util.ChartUtil.NumberOnlyBuildLabel;
import hudson.util.ColorPalette;
import hudson.util.ShiftedCategoryAxis;
import hudson.util.StackedAreaRenderer2;

import java.awt.Color;
import java.awt.Paint;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.tap4j.plugin.TapBuildAction;
import org.tap4j.plugin.TapResult;
import org.tap4j.plugin.AbstractTapProjectAction;

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
          null,                     // chart title
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
			return  label.build.getNumber() + "/" + AbstractTapProjectAction.URL_NAME + "/";
        }

          @Override
        public String generateToolTip(CategoryDataset dataset, int row, int column) 
        {
              NumberOnlyBuildLabel label = (NumberOnlyBuildLabel) dataset.getColumnKey(column);
              TapBuildAction build = label.build.getAction(TapBuildAction.class);
              TapResult report = build.getResult();
              report.updateStats();

              switch (row) {
                  case 0:
                      return String.valueOf(report.getFailed()) + " Failure(s)";
                  case 1:
                     return String.valueOf(report.getPassed()) + " Pass";
                  case 2:
                     return String.valueOf(report.getSkipped()) + " Skip(s)";
                  default:
                     return "";
              }
          }

      };

      plot.setRenderer(ar);
      ar.setSeriesPaint(0, ColorPalette.RED); // Failures
      ar.setSeriesPaint(1, ColorPalette.BLUE); // Pass
      ar.setSeriesPaint(2, ColorPalette.YELLOW); // Skips

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
	 *            a map with build as key and the test methods execution status
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
			Map<String, Paint> statusPaintMap = new HashMap<String, Paint>();

			{
				statusPaintMap.put("PASS", ColorPalette.BLUE);
				statusPaintMap.put("SKIP", ColorPalette.YELLOW);
				statusPaintMap.put("FAIL", ColorPalette.RED);
			}

			/**
			 * Returns the paint for an item. Overrides the default behavior
			 * inherited from AbstractSeriesRenderer.
			 * 
			 * @param row
			 *            the series.
			 * @param column
			 *            the category.
			 * 
			 * @return The item color.
			 */
			public Paint getItemPaint( final int row, final int column )
			{
				NumberOnlyBuildLabel label = (NumberOnlyBuildLabel) dataset
						.getColumnKey(column);
				Paint paint = statusPaintMap.get(statusMap.get(label));
				// when the status of test method is unknown, use gray color
				return paint == null ? Color.gray : paint;
			}
		};

		br.setBaseToolTipGenerator(new CategoryToolTipGenerator()
		{
			public String generateToolTip( CategoryDataset dataset, int row,
					int column )
			{
				NumberOnlyBuildLabel label = (NumberOnlyBuildLabel) dataset
						.getColumnKey(column);
				if ("UNKNOWN".equals(statusMap.get(label)))
				{
					return "unknown";
				}
				// values are in seconds
				return dataset.getValue(row, column) + " secs";
			}
		});

		br.setBaseItemURLGenerator(new CategoryURLGenerator()
		{
			public String generateURL( CategoryDataset dataset, int series,
					int category )
			{
				NumberOnlyBuildLabel label = (NumberOnlyBuildLabel) dataset
						.getColumnKey(category);
				if ("UNKNOWN".equals(statusMap.get(label)))
				{
					// no link when method result doesn't exist
					return null;
				}
				// return label.build.getUpUrl() + label.build.getNumber() + "/" + PluginImpl.URL + "/" + methodUrl;
				return label.build.getUpUrl() + label.build.getNumber() + "/tap/" + methodUrl;
			}
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
