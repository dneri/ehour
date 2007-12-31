/**
 * Created on Sep 12, 2007
 * Created by Thies Edeling
 * Created by Thies Edeling
 * Copyright (C) 2007 TE-CON, All Rights Reserved.
 *
 * This Software is copyright TE-CON 2007. This Software is not open source by definition. The source of the Software is available for educational purposes.
 * TE-CON holds all the ownership rights on the Software.
 * TE-CON freely grants the right to use the Software. Any reproduction or modification of this Software, whether for commercial use or open source,
 * is subject to obtaining the prior express authorization of TE-CON.
 * 
 * thies@te-con.nl
 * TE-CON
 * Legmeerstraat 4-2h, 1058ND, AMSTERDAM, The Netherlands
 *
 */

package net.rrm.ehour.ui.panel.report;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import net.rrm.ehour.config.EhourConfig;
import net.rrm.ehour.ui.border.GreyBlueRoundedBorder;
import net.rrm.ehour.ui.model.CurrencyModel;
import net.rrm.ehour.ui.model.DateModel;
import net.rrm.ehour.ui.model.FloatModel;
import net.rrm.ehour.ui.report.TreeReport;
import net.rrm.ehour.ui.report.node.ReportNode;
import net.rrm.ehour.ui.session.EhourWebSession;
import net.rrm.ehour.ui.util.HtmlUtil;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.resources.CompressedResourceReference;
import org.apache.wicket.markup.html.resources.StyleSheetReference;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.value.ValueMap;

/**
 * Aggregate report data panel
 **/

public class TreeReportDataPanel extends Panel
{
	private static final long serialVersionUID = -6757047600645464803L;
	private static final Logger	logger = Logger.getLogger(TreeReportDataPanel.class);
	private TreeReportColumn[]	reportColumns;
	
	/**
	 * Default constructor 
	 * @param id
	 * @param report report data
	 */
	public TreeReportDataPanel(String id, TreeReport report, ReportType reportType, String excelResourceName)
	{
		super(id);
		
		GreyBlueRoundedBorder blueBorder = new GreyBlueRoundedBorder("blueFrame");
		add(blueBorder);
		
		if (excelResourceName != null)
		{
			final String reportId = report.getReportId();
			
			ResourceReference excelResource = new ResourceReference(excelResourceName);
			ValueMap params = new ValueMap();
			params.add("reportId", reportId);
			ResourceLink excelLink = new ResourceLink("excelLink", excelResource, params);
			add(excelLink);

			EhourConfig config = ((EhourWebSession)getSession()).getEhourConfig();
			
			add(new Label("reportHeader",new StringResourceModel("report.header", 
											this, null, 
													new Object[]{new DateModel(report.getReportRange().getDateStart(), config),
										 			new DateModel(report.getReportRange().getDateEnd(), config)})));		
		}
		else
		{
			add(HtmlUtil.getInvisibleLink("excelLink"));
			add(HtmlUtil.getInvisibleLabel("reportHeader"));
		}
		
		initReportColumns(reportType);
		
		addHeaderColumns(blueBorder);
		addReportData(report, blueBorder);
		addGrandTotal(report, blueBorder);
		
		add(new StyleSheetReference("reportStyle", new CompressedResourceReference(this.getClass(), "style/reportStyle.css")));
	}
	
	/**
	 * Grand total row
	 * @param report
	 * @param parent
	 */
	private void addGrandTotal(TreeReport report, WebMarkupContainer parent)
	{
		RepeatingView	totalView = new RepeatingView("cell");
		int				i = 0;
		float			hours = 0;
		float			turnOver = 0;

		EhourConfig config = ((EhourWebSession)this.getSession()).getEhourConfig();
		
		// get totals
		for (ReportNode node : report.getNodes())
		{
			turnOver += node.getTurnover().floatValue();
			hours += node.getHours().floatValue();
		}
		
		// add cells
		totalView.add(new Label(Integer.toString(i++), new ResourceModel("report.total")));
		
		for (; i < reportColumns.length; i++)
		{
			if (reportColumns[i].isVisible())
			{
				Label label = null;
				
				if (reportColumns[i].getColumnType() == TreeReportColumn.ColumnType.HOUR)
				{
					label = new Label(Integer.toString(i), new FloatModel(hours, config));
				}
				else if (reportColumns[i].getColumnType() == TreeReportColumn.ColumnType.TURNOVER)
				{
					label = new Label(Integer.toString(i), new CurrencyModel(turnOver, config));
					label.setEscapeModelStrings(false);
				}
				else
				{
					label = HtmlUtil.getNbspLabel(Integer.toString(i));
				}
				
				addColumnTypeStyling(reportColumns[i].getColumnType(), label);
				totalView.add(label);
			}
		}
		
		parent.add(totalView);
	}
	
	/**
	 * Add report data table to the component
	 * @param report
	 * @param parent
	 */
	private void addReportData(TreeReport report, WebMarkupContainer parent)
	{
		@SuppressWarnings("serial")
		ListView rootNodeView = new ListView("reportData", report.getNodes())
		{
			@Override
			protected void populateItem(ListItem item)
			{
				ReportNode rootNode = (ReportNode)item.getModelObject();

				item.add(getReportNodeRows(rootNode));
				item.add(getTotalRow(rootNode));
			}
		};
		
		parent.add(rootNodeView);
	}

	/**
	 * Add the total row for a block (root node)
	 * @param reportNode
	 * @return
	 */
	private Component getTotalRow(ReportNode reportNode)
	{
		RepeatingView	totalView = new RepeatingView("cell");
		
		EhourConfig config = ((EhourWebSession)this.getSession()).getEhourConfig();
		
		int i = 0;
		
		for (TreeReportColumn column : reportColumns)
		{
			if (column.isVisible())
			{
				String	id = Integer.toString(i++);
				
				if (column.getColumnType() == TreeReportColumn.ColumnType.HOUR)
				{
					totalView.add(new Label(id, new FloatModel(reportNode.getHours(), config)));
				}
				else if (column.getColumnType() == TreeReportColumn.ColumnType.TURNOVER)
				{
					Label label = new Label(id, new CurrencyModel(reportNode.getTurnover(), config));
					label.setEscapeModelStrings(false);
					totalView.add(label);
				}
				else
				{
					totalView.add(HtmlUtil.getNbspLabel(id));
				}
			}
		}
		
		return totalView;
	}
	
	/**
	 * Get root node rows & cells
	 * @param reportNode
	 * @return
	 */
	private Component getReportNodeRows(ReportNode reportNode)
	{
		Serializable[][] matrix = reportNode.getNodeMatrix(reportColumns.length);
	
		// add rows per node
		@SuppressWarnings("serial")
		ListView rootNodeView = new ListView("row", Arrays.asList(matrix))
		{
			@Override
			protected void populateItem(ListItem item)
			{
				RepeatingView cells = new RepeatingView("cell");
				Serializable[] rowValues = (Serializable[])item.getModelObject();
				int i = 0;
				
				// add cells for a row
				for (Serializable cellValue : rowValues)
				{
					if (reportColumns[i].isVisible())
					{
						Label cellLabel;
					
						if (reportColumns[i].getConversionModel() == null)
						{
							cellLabel = new Label(Integer.toString(i), new Model(cellValue));
						}
						else
						{
							IModel model;

							try
							{
								model = getModelInstance(reportColumns[i]);
								model.setObject(cellValue);
							} catch (Exception e)
							{
								logger.warn("Could not instantiate model for " + reportColumns[i], e);
								model = new Model(cellValue);
							}
							
							cellLabel = new Label(Integer.toString(i), model);
							addColumnTypeStyling(reportColumns[i].getColumnType(), cellLabel);
							
						}
						
						cells.add(cellLabel);
					}
					
					i++;
				}
				
				item.add(cells);
				
				if (item.getIndex() % 2 == 1)
				{
					item.add(new SimpleAttributeModifier("style", "background-color: #fefeff"));
				}
			}
			
		};

		return rootNodeView;
	}

	/**
	 * Get a model instance
	 * @param columnHeader
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 */
	@SuppressWarnings("unchecked")
	private IModel getModelInstance(TreeReportColumn columnHeader) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException
	{
		IModel model = null;
		
		if (columnHeader.getConversionModelConstructorParams() == null)
		{
			model = columnHeader.getConversionModel().newInstance();
		}
		else
		{
			Constructor[]	constructors = columnHeader.getConversionModel().getConstructors();
			
			for (Constructor constructor : constructors)
			{
				// let's not make it too complex, just check argument length and not check types..
				if (constructor.getParameterTypes().length == columnHeader.getConversionModelConstructorParams().length)
				{
					model = (IModel)constructor.newInstance(columnHeader.getConversionModelConstructorParams());
					break;
				}
			}
		}
		
		return model;
	}
	
	/**
	 * Add header columns to parent
	 * @param parent
	 */
	private void addHeaderColumns(WebMarkupContainer parent)
	{
		RepeatingView	columnHeaders = new RepeatingView("columnHeaders");
		int				i = 0;
		
		for (TreeReportColumn treeReportColumn : reportColumns)
		{
			Label columnHeader = new Label(Integer.toString(i++), new ResourceModel(treeReportColumn.getColumnHeaderResourceKey()));
			columnHeader.setVisible(treeReportColumn.isVisible());
			columnHeaders.add(columnHeader);
			addColumnTypeStyling(treeReportColumn.getColumnType(), columnHeader);
			
			logger.debug("Adding report columnheader " + treeReportColumn.getColumnHeaderResourceKey() + ", visible: " +  columnHeader.isVisible());
		}
		
		parent.add(columnHeaders);
	}
	
	/**
	 * Add column type specific styling
	 * @param columnType
	 * @param label
	 */
	private void addColumnTypeStyling(TreeReportColumn.ColumnType columnType, Label label)
	{
		if (columnType != TreeReportColumn.ColumnType.OTHER && label != null)
		{
			label.add(new SimpleAttributeModifier("style", "text-align: right"));
		}
	}
	
	/**
	 * 
	 * @param reportType
	 * @return
	 */
	private void initReportColumns(ReportType reportType)
	{
		EhourConfig config = ((EhourWebSession)this.getSession()).getEhourConfig();
		
		reportColumns = ReportColumnUtil.getReportColumns(config, reportType);
	}
}