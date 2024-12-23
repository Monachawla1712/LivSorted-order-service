package com.sorted.rest.services.order.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.DateUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.services.order.beans.InvoiceDataBean;
import com.sorted.rest.services.order.clients.ClientService;
import com.sorted.rest.services.order.constants.OrderConstants.InvoiceType;
import com.sorted.rest.services.order.entity.FranchiseOrderEntity;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;
import com.sorted.rest.services.order.services.FranchiseOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

@Component("pdfGenerator")
public class InvoicePDFGenerator extends PdfPageEventHelper {

	private AppLogger _LOGGER = LoggingManager.getLogger(InvoicePDFGenerator.class);

	@Autowired
	FranchiseOrderService franchiseOrderService;

	@Autowired
	ClientService clientService;

	private static final Font COURIER = new Font(Font.FontFamily.COURIER, 8, Font.BOLD, BaseColor.BLACK);

	private static final Font COURIER_SMALL = new Font(Font.FontFamily.COURIER, 5);

	private static final Font COURIER_SMALL_FOOTER = new Font(Font.FontFamily.COURIER, 8, Font.BOLD);

	private static final Font DOCUMENT_HEADER = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);

	private static final Font HEADER_CONTENT_FONT = new Font(Font.FontFamily.COURIER, 8, Font.BOLD);

	private static final Font TABLE_CONTENT_FONT = new Font(Font.FontFamily.COURIER, 8, Font.NORMAL, BaseColor.BLUE);

	private static final Font TABLE_CONTENT_FONT_HINDI = FontFactory.getFont("fonts/mangal.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 8, Font.NORMAL,
			BaseColor.BLUE);

	public synchronized void generatePdfReport(InvoiceDataBean invoiceData) {
		Document document = new Document();
		try {
			String s = System.getProperty("user.dir");
			String invoiceFileName = invoiceData.getInvoiceId();
			PdfWriter.getInstance(document, new FileOutputStream(getPdfNameWithDate(s, invoiceFileName)));
			document.open();
			prepareInvoicePdf(document, invoiceData);
			document.close();
		} catch (FileNotFoundException | DocumentException e) {
			_LOGGER.error("FileNotFoundException | DocumentException  in generatePdfReport invoice:  ", e);
		} catch (IOException e) {
			_LOGGER.error("IOException in generatePdfReport invoice:", e);
			throw new RuntimeException(e);
		}

	}

	private void prepareInvoicePdf(Document document, InvoiceDataBean invoiceData) throws DocumentException, IOException {
		addLogo(document, invoiceData);
		addDocTitle(document, invoiceData);
		showSKusDetails(document, invoiceData);
		addFooter(document, invoiceData);
	}

	private void addLogo(Document document, InvoiceDataBean invoiceData) {
		try {
			PdfPTable table = new PdfPTable(1);
			table.setWidthPercentage(100);
			table.getDefaultCell().setPadding(5);
			table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
			table.getDefaultCell().setVerticalAlignment(Element.ALIGN_CENTER);
			if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
				addLogoCell(table, "Bill of Supply", BaseColor.LIGHT_GRAY);
			} else {
				addLogoCell(table, convertToTitleCase(invoiceData.getInvoiceType().toString().replace("FRANCHISE_", "")), BaseColor.LIGHT_GRAY);
			}
			addLogoCell(table, "BCFD Technologies Private limited", new BaseColor(175, 203, 107));
			document.add(table);
		} catch (DocumentException e) {
			_LOGGER.error("DocumentException  in addLogo in invoice:  ", e);
		}
	}

	private void addLogoCell(PdfPTable table, String cellText, BaseColor cellColor) {
		Paragraph p = new Paragraph();
		p.setFont(DOCUMENT_HEADER);
		p.add(cellText);
		PdfPCell cell = new PdfPCell(p);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_CENTER);
		cell.setBackgroundColor(cellColor);
		table.addCell(cell);
	}

	private void addDocTitle(Document document, InvoiceDataBean invoiceData) throws DocumentException {
		PdfPTable table = new PdfPTable(1);
		table.setWidthPercentage(100);
		table.getDefaultCell().setPadding(5);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		table.getDefaultCell().setVerticalAlignment(Element.ALIGN_CENTER);
		Paragraph p = new Paragraph();
		leaveEmptyLine(p, 1);

		String adjustmentList = ParamsUtils.getParam("HEADING_DETAILS",
				"Address:Ground Floor, Plot no 129, Deshbandhu Sir Choturam Bhawan,\nShaheed Major Vikash Yadav Marg, Jharsa, Gurugram, Gurugram,\nHaryana, 122003\n\t\t\t|Contact:_|PAN:AAKCB5278N|GST:06AAKCB5278N1Z2");
		LinkedHashMap<String, String> headingDetails = new LinkedHashMap<>();
		for (String adjustmentPairString : adjustmentList.split("\\|")) {
			List<String> KeyValuePair = Arrays.asList(adjustmentPairString.split(":"));
			headingDetails.put(KeyValuePair.get(0), KeyValuePair.get(1));
		}
		for (Map.Entry<String, String> adjustment : headingDetails.entrySet()) {
			p.add(new Paragraph(adjustment.getKey() + " # :- " + adjustment.getValue(), HEADER_CONTENT_FONT));
			p.setAlignment(Element.ALIGN_CENTER);
		}
		leaveEmptyLine(p, 1);
		table.addCell(p);
		document.add(p);

		PdfPTable table2 = new PdfPTable(2);
		table2.setWidthPercentage(100);

		Paragraph leftParagraph = new Paragraph();

		addParagraphInNewLine(leftParagraph, "INVOICE BILL TO :- State Code:- 06", Element.ALIGN_CENTER, HEADER_CONTENT_FONT);
		addParagraphInNewLine(leftParagraph, "Name of Recipient :- ".concat(invoiceData.getRecipientName()), Element.ALIGN_CENTER, HEADER_CONTENT_FONT);
		addParagraphInNewLine(leftParagraph, "Address :- ".concat(invoiceData.getAddress()), Element.ALIGN_CENTER, HEADER_CONTENT_FONT);
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			addParagraphInNewLine(leftParagraph, "Regular Invoice Number :- ".concat(invoiceData.getInvoiceName()), Element.ALIGN_CENTER, HEADER_CONTENT_FONT);
			addParagraphInNewLine(leftParagraph, "Invoice Date :- ".concat(invoiceData.getDeliveryDate().toString().split("\\s+")[0]), Element.ALIGN_CENTER,
					HEADER_CONTENT_FONT);
			if (invoiceData.getStorePanNumber() != null) {
				addParagraphInNewLine(leftParagraph, "PAN :- ".concat(invoiceData.getStorePanNumber()), Element.ALIGN_CENTER, HEADER_CONTENT_FONT);
			}
			if (invoiceData.getStoreGstNumber() != null) {
				addParagraphInNewLine(leftParagraph, "GST :- ".concat(invoiceData.getStoreGstNumber()), Element.ALIGN_CENTER, HEADER_CONTENT_FONT);
			}
		}
		leaveEmptyLine(leftParagraph, 1);

		leftParagraph.setAlignment(Element.ALIGN_LEFT);
		PdfPCell leftCell = new PdfPCell(leftParagraph);
		leftCell.setBorder(Rectangle.NO_BORDER);
		leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		Paragraph rightParagraph = new Paragraph();
		if (!Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			addParagraphInNewLine(rightParagraph, "DATE OF ISSUE :- ".concat(sdf.format(DateUtils.convertDateUtcToIst(DateUtils.addDays(new Date(), 0)))),
					Element.ALIGN_RIGHT, HEADER_CONTENT_FONT);
			addParagraphInNewLine(rightParagraph, convertToTitleCase(invoiceData.getInvoiceType().toString().replace("FRANCHISE_", "")) + " No. :- ".concat(invoiceData.getInvoiceName()),
					Element.ALIGN_RIGHT, HEADER_CONTENT_FONT);
			addParagraphInNewLine(rightParagraph,
					"ORIGINAL INVOICE DATE:- ".concat(invoiceData.getParentInvoice().getDeliveryDate().toString().split("\\s+")[0]), Element.ALIGN_RIGHT,
					HEADER_CONTENT_FONT);
			addParagraphInNewLine(rightParagraph, "Regular Invoice Number :- ".concat(invoiceData.getParentInvoice().getInvoiceName()), Element.ALIGN_RIGHT,
					HEADER_CONTENT_FONT);
			if (invoiceData.getStorePanNumber() != null) {
				addParagraphInNewLine(leftParagraph, "PAN :- ".concat(invoiceData.getStorePanNumber()), Element.ALIGN_CENTER, HEADER_CONTENT_FONT);
			}
			if (invoiceData.getStoreGstNumber() != null) {
				addParagraphInNewLine(leftParagraph, "GST :- ".concat(invoiceData.getStoreGstNumber()), Element.ALIGN_CENTER, HEADER_CONTENT_FONT);
			}
			leaveEmptyLine(rightParagraph, 1);
		}
		rightParagraph.setAlignment(Element.ALIGN_RIGHT);
		PdfPCell rightCell = new PdfPCell(rightParagraph);
		rightCell.setBorder(Rectangle.NO_BORDER);
		rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

		table2.addCell(leftCell);
		table2.addCell(rightCell);
		document.add(table2);
		document.add(new Paragraph("\n\n"));
	}

	public static String convertToTitleCase(String input) {
		String[] parts = input.split("_");
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			sb.append(Character.toUpperCase(part.charAt(0)));
			sb.append(part.substring(1).toLowerCase());
			sb.append(" ");
		}
		return sb.toString().trim();
	}

	private void addParagraphInNewLine(Paragraph paragraph, String text, int Alignment, Font font) {
		paragraph.add(new Paragraph(text, font));
		paragraph.setAlignment(Alignment);
		paragraph.add(new Chunk("\n"));
	}

	private void showSKusDetails(Document document, InvoiceDataBean invoiceData) throws DocumentException, IOException {
		List<String> columnNames = getColumnNames(invoiceData.getIsSrpStore());
		int noOfColumns = columnNames.size();
		float[] columnWidths = { 50F, 150F, 60F, 50F, 55F, 50F, 50F, 55F, 50F };
		if (invoiceData.getIsSrpStore() != null && invoiceData.getIsSrpStore().equals(1)) {
			columnWidths = new float[] { 50F, 150F, 60F, 50F, 50F, 55F, 50F, 50F, 55F, 50F };
		}
		PdfPTable table = new PdfPTable(noOfColumns);
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			Paragraph paragraph = new Paragraph();
			paragraph.setFont(COURIER_SMALL_FOOTER);
			leaveEmptyLine(paragraph, 3);

			table.setTotalWidth(525);
			table.setWidths(columnWidths);
			table.setLockedWidth(true);
			for (String columnName : columnNames) {
				table.getDefaultCell().setPadding(3);
				addParagraphToTable(table, columnName, HEADER_CONTENT_FONT);
			}

			table.setHeaderRows(1);
			getDbData(table, invoiceData);
			showfinalBillingDetails(table, invoiceData);
			document.add(table);

			createGap(document);
		} else if (invoiceData.getIsRefundInvoice() == 1 && invoiceData.getRefundOrders() != null) {
			showRefundOrders(document, invoiceData, columnWidths, columnNames);
		}

		if (invoiceData.getIsRefundInvoice() != 1) {
			PdfPTable table3;
			if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
				table3 = new PdfPTable(noOfColumns);
				table3.setTotalWidth(525);
				table3.setWidths(columnWidths);
				table3.setLockedWidth(true);
				for (int i = 0; i < noOfColumns; i++) {
					table.getDefaultCell().setPadding(3);
					Paragraph p = new Paragraph();
					p.setFont(HEADER_CONTENT_FONT);
					table3.addCell(p);
				}
				table3.setHeaderRows(1);
				showAdjustmentsData(table3, invoiceData);
				document.add(table3);
			} else {
				table3 = new PdfPTable(2);
				table3.setTotalWidth(225);
				columnWidths = new float[] { 150F, 75F };
				table3.setWidths(columnWidths);
				table3.setLockedWidth(true);
				showAdjustmentsData(table3, invoiceData);
				table3.setHorizontalAlignment(Element.ALIGN_LEFT);
				document.add(table3);
			}
		}

		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			if (invoiceData.getShowPackingDetails().equals(1) && !invoiceData.getPackingSkusDetails().isEmpty()) {
				createGap(document);
				showPackingDetails(document, invoiceData, columnWidths, columnNames);
			}
			createGap(document);
			if (invoiceData.getRefundOrders() != null) {
				showRefundOrders(document, invoiceData, columnWidths, columnNames);
			}
		}
	}

	private List<String> getColumnNames(Integer isSrpStore) {
		List<String> columnNames = new ArrayList<>();
		columnNames.add("SKU ID");
		columnNames.add("Product Description");
		columnNames.add("HSN");
		if (isSrpStore != null && isSrpStore.equals(1)) {
			columnNames.add("MARGIN");
			columnNames.add("SRP");
		} else {
			columnNames.add("SPP");
		}
		columnNames.add("UOM");
		columnNames.add("Qty");
		columnNames.add("Gross Amount");
		columnNames.add("Discount");
		columnNames.add("Net Amount");
		return columnNames;
	}

	private PdfPTable createTable(Document document, float[] columnWidths, List<String> columnNames, String tableHeading) throws DocumentException {
		Paragraph p = new Paragraph();
		leaveEmptyLine(p, 1);
		p.setAlignment(Element.ALIGN_CENTER);
		p.add(new Paragraph(tableHeading, COURIER_SMALL_FOOTER));
		leaveEmptyLine(p, 1);
		document.add(p);

		PdfPTable table = new PdfPTable(columnNames.size());
		table.setTotalWidth(525);
		table.setWidths(columnWidths);
		table.setLockedWidth(true);
		for (String columnName : columnNames) {
			table.getDefaultCell().setPadding(3);
			Paragraph p2 = new Paragraph();
			p2.setFont(HEADER_CONTENT_FONT);
			p2.add(columnName);
			table.addCell(p2);
		}
		table.setHeaderRows(1);
		return table;
	}

	private void showPackingDetails(Document document, InvoiceDataBean invoiceData, float[] columnWidths, List<String> columnNames) throws DocumentException {
		PdfPTable table = createTable(document, columnWidths, columnNames, "Packing Details ");
		addItemDetails(table, invoiceData.getPackingSkusDetails(), invoiceData.getIsSrpStore(), 0);
		setItemListFooter(table, "Total", invoiceData.getIsSrpStore(), invoiceData.getPackingTotalQty(),
				invoiceData.getAdjustmentsMap().get("PACKING-CHARGES").getAmount(), BigDecimal.valueOf(0d),invoiceData.getPackingTotalCharges());
		document.add(table);
	}

	private void createGap(Document document) throws DocumentException {
		Paragraph p = new Paragraph();
		leaveEmptyLine(p, 1);
		p.setAlignment(Element.ALIGN_LEFT);
		p.add(new Paragraph("", COURIER_SMALL_FOOTER));
		document.add(p);
	}

	private void showfinalBillingDetails(PdfPTable table, InvoiceDataBean invoiceData) {
		PdfPCell cell = new PdfPCell();
		if (invoiceData.getIsSrpStore() != null && invoiceData.getIsSrpStore().equals(1)) {
			cell = new PdfPCell(new Paragraph("(-)Total Margin Discount", COURIER));
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			cell.setIndent(2);
			cell.setColspan(8);
			table.addCell(cell);
			addParagraphToTable(table, "", TABLE_CONTENT_FONT);
			addParagraphToTable(table, invoiceData.getTotalDiscountAmount().toString(), TABLE_CONTENT_FONT);
		}
		if (invoiceData.getOfferType() != null && Objects.equals(invoiceData.getOfferType(), "ORDER") && invoiceData.getOfferDiscountAmount() != null) {
			cell = new PdfPCell(new Paragraph("Offer Discount:-", COURIER));
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			cell.setIndent(2);
			if (invoiceData.getIsSrpStore() != null && invoiceData.getIsSrpStore().equals(1)) {
				cell.setColspan(8);
			} else {
				cell.setColspan(7);
			}
			table.addCell(cell);
			addParagraphToTable(table, invoiceData.getOfferDiscountAmount().setScale(2, RoundingMode.HALF_UP).toString(), TABLE_CONTENT_FONT);
			addParagraphToTable(table, "", TABLE_CONTENT_FONT);
		}
		cell = new PdfPCell(new Paragraph("Total", COURIER));
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cell.setIndent(2);
		if (invoiceData.getIsSrpStore() != null && invoiceData.getIsSrpStore().equals(1)) {
			cell.setColspan(8);
		} else {
			cell.setColspan(7);
		}
		table.addCell(cell);
		addParagraphToTable(table, "", TABLE_CONTENT_FONT);
		addParagraphToTable(table, invoiceData.getDisplayFinalAmount().toString(), TABLE_CONTENT_FONT);
	}

	private void addParagraphToTable(PdfPTable table, String paragraphText, Font fontType) {
		Paragraph p = new Paragraph();
		p.setFont(fontType);
		p.add(paragraphText);
		table.addCell(p);
	}

	private void addItemDetails(PdfPTable table, List<FranchiseOrderItemEntity> orderItems, Integer isSrpStore, Integer isRefundOrder) {
		for (FranchiseOrderItemEntity franchiseOrderItem : orderItems) {
			table.setWidthPercentage(100);
			table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
			table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.getDefaultCell().setPadding(3);
			table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getSkuCode(), TABLE_CONTENT_FONT)));
			table.addCell(new PdfPCell(getBilingualParagraph(franchiseOrderItem.getProductName())));
			table.addCell(new PdfPCell(new Paragraph((franchiseOrderItem.getHsn() != null ? franchiseOrderItem.getHsn() : ""), TABLE_CONTENT_FONT)));
			if (isSrpStore != null && isSrpStore.equals(1)) {
				table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getMarginDiscountPercent().toString().concat("%"), TABLE_CONTENT_FONT)));
			}
			table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getSalePrice().toString(), TABLE_CONTENT_FONT)));
			table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getUom(), TABLE_CONTENT_FONT)));
			table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getFinalQuantity().toString(), TABLE_CONTENT_FONT)));
			if (isRefundOrder == null || isRefundOrder == 0) {
				table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getMrpGrossAmount().toString(), TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph(getItemDiscount(franchiseOrderItem).toString(), TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getSpGrossAmount().toString(), TABLE_CONTENT_FONT)));
			} else {
				table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getFinalAmount().toString(), TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph(BigDecimal.valueOf(0).toString(), TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getFinalAmount().toString(), TABLE_CONTENT_FONT)));
			}
		}
	}

	private BigDecimal getItemDiscount(FranchiseOrderItemEntity franchiseOrderItem) {
		BigDecimal offerItemDiscount = (franchiseOrderItem.getOfferDiscountAmount() != null) ? franchiseOrderItem.getOfferDiscountAmount() : BigDecimal.ZERO;
		return BigDecimal.valueOf(franchiseOrderItem.getMrpGrossAmount()).subtract(BigDecimal.valueOf(franchiseOrderItem.getSpGrossAmount())).add(offerItemDiscount);
	}

	private void getDbData(PdfPTable table, InvoiceDataBean invoiceData) {
		addItemDetails(table, invoiceData.getOrderItems(), invoiceData.getIsSrpStore(), invoiceData.getIsRefundInvoice());
		setItemListFooter(table, "Sub Total", invoiceData.getIsSrpStore(),invoiceData.getDisplayFinalQty(),invoiceData.getSubAmtTotal(),getTotalItemDiscount(invoiceData),invoiceData.getTotalMrpGrossAmount());
	}

	private void setItemListFooter(PdfPTable table, String totalText, Integer isSrpStore, BigDecimal quantity, BigDecimal amount,BigDecimal totalDiscount,BigDecimal totalMrp) {
		PdfPCell cell = new PdfPCell(new Paragraph(totalText + ":-", COURIER));
		cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cell.setIndent(2);
		if (isSrpStore != null && isSrpStore.equals(1)) {
			cell.setColspan(6);
		} else {
			cell.setColspan(5);
		}
		table.addCell(cell);

		table.addCell(new Paragraph(quantity.toString(), TABLE_CONTENT_FONT));
		table.addCell(new Paragraph(totalMrp.toString(), TABLE_CONTENT_FONT));
		table.addCell(new Paragraph(totalDiscount.toString(), TABLE_CONTENT_FONT));
		table.addCell(new Paragraph(amount.toString(), TABLE_CONTENT_FONT));
	}

	private BigDecimal getTotalItemDiscount(InvoiceDataBean invoiceData) {
		BigDecimal offerDiscount = BigDecimal.ZERO;
		if (invoiceData.getOfferDiscountAmount() != null && Objects.equals(invoiceData.getOfferType(), "ORDERITEM")) {
			offerDiscount = offerDiscount.add(invoiceData.getOfferDiscountAmount());
		}
		return invoiceData.getTotalMrpGrossAmount().subtract(invoiceData.getSubAmtTotal()).add(offerDiscount);
	}
	private Paragraph getBilingualParagraph(String productName) {
		Paragraph paragraph = new Paragraph();
		for (String word : productName.split("\\s+")) {
			if (checkIfHindiContent(word)) {
				Chunk chunk = new Chunk(word + " ", TABLE_CONTENT_FONT_HINDI);
				paragraph.add(chunk);
			} else {
				Chunk chunk = new Chunk(word + " ", TABLE_CONTENT_FONT);
				paragraph.add(chunk);
			}
		}
		return paragraph;
	}

	private boolean checkIfHindiContent(String word) {
		if (Character.UnicodeBlock.of(word.charAt(0)) == Character.UnicodeBlock.DEVANAGARI || word.length() == 1) {
			return true;
		}
		if (word.charAt(0) == '(' && word.charAt(word.length() - 1) == ')') {
			return Character.UnicodeBlock.of(word.charAt(1)) == Character.UnicodeBlock.DEVANAGARI;
		}
		return false;
	}

	private void showRefundOrders(Document document, InvoiceDataBean invoiceData, float[] columnWidths, List<String> columnNames) throws DocumentException {
		for (FranchiseOrderEntity refundOrder : invoiceData.getRefundOrders()) {
			PdfPTable table = createTable(document, columnWidths, columnNames,
					"Refund Order (".concat(refundOrder.getRefundType() != null ? refundOrder.getRefundType() : "").concat(")"));
			addItemDetails(table, refundOrder.getOrderItems(), invoiceData.getIsSrpStore(), 1);
			setItemListFooter(table, "Total", invoiceData.getIsSrpStore(), invoiceData.getRefundQtyTotals().get(refundOrder.getId()).getTotalQty(),
					invoiceData.getRefundQtyTotals().get(refundOrder.getId()).getTotalAmt(),
					BigDecimal.valueOf(0d),invoiceData.getRefundQtyTotals().get(refundOrder.getId()).getTotalAmt());
			document.add(table);
		}
	}

	private void showAdjustmentsData(PdfPTable table, InvoiceDataBean invoiceData) {
		PdfPCell cell = new PdfPCell(new Paragraph(Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE) ? "Adjustments" : "Particulars", COURIER));
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		} else {
			cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		}
		cell.setIndent(2);
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			if (invoiceData.getIsSrpStore() != null && invoiceData.getIsSrpStore().equals(1)) {
				cell.setColspan(9);
			} else {
				cell.setColspan(8);
			}
		}
		table.addCell(cell);
		addParagraphToTable(table, Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE) ? "" : "Amount", HEADER_CONTENT_FONT);
		createAdjustmentsCell(table, invoiceData);
	}

	private void createAdjustmentsCell(PdfPTable table, InvoiceDataBean invoiceData) {
		String adjustmentList = ParamsUtils.getParam("INVOICE_MAP",
				"FO-DELIVERY-CHARGE:Delivery Charges,PACKING-CHARGES:Packing Charges,FO-OT-CHARGE:OT Charges,FO-LD-REFUND:Late delivery Refund,FO-UNLOADING-CHARGE:Unloading Charges,FO-SHORT-QUANTITY-REFUND:Short Quantity Refund,FO-RETURN-QUANTITY-REFUND:Return Quantity Refund,FO-EXTRA-MARGIN-DISCOUNT:Extra Margin Discount,FO-CRATE-ADJUSTMENT:Crate Adjustment,FO-OTHER-CHARGE:Other Charges,FO-OTHER-REFUND:Other Refunds");
		LinkedHashMap<String, String> adjustmentKeys = new LinkedHashMap<>();
		for (String adjustmentPairString : adjustmentList.split(",")) {
			List<String> KeyValuePair = Arrays.asList(adjustmentPairString.split(":"));
			adjustmentKeys.put(KeyValuePair.get(0), KeyValuePair.get(1));
		}
		LinkedHashMap<String, String> cellsMap = new LinkedHashMap<>();
		for (Map.Entry<String, String> adjustment : adjustmentKeys.entrySet()) {
			BigDecimal amount = new BigDecimal("0.00");
			if (invoiceData.getAdjustmentsMap().containsKey(adjustment.getKey()) && !invoiceData.getAdjustmentsMap().get(adjustment.getKey()).getAmount()
					.equals(BigDecimal.ZERO)) {
				amount = invoiceData.getAdjustmentsMap().get(adjustment.getKey()).getAmount();
			}
			if (!Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE) && amount.compareTo(BigDecimal.valueOf(0)) == 0) {
				continue;
			}
			String sign = "";
			if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
				sign = (amount.compareTo(new BigDecimal("0.00")) >= 0) ? "(+) " : "(-) ";
			}
			cellsMap.put(sign + adjustment.getValue(), amount.abs().toString());
		}
		for (Map.Entry<String, String> cellMap : cellsMap.entrySet()) {
			addRightAlignedCell(cellMap.getKey(), invoiceData, cellMap.getValue(), table);
		}
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			String totalAdjustmentsText = "0.00";
			if (invoiceData.getTotalAdjustments() != null && !invoiceData.getTotalAdjustments().equals(BigDecimal.ZERO)) {
				totalAdjustmentsText = (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) ?
						invoiceData.getTotalAdjustments().toString() :
						invoiceData.getTotalAdjustments().abs().toString();
			}
			addRightAlignedCell("Total Adjustments", invoiceData, totalAdjustmentsText, table);
		}
		String totalAmountText = "0.00";
		if ((invoiceData.getTotalAmount() != null)) {
			totalAmountText = (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) ?
					invoiceData.getTotalAmount().toString() :
					invoiceData.getTotalAmount().abs().toString();
		}
		addRightAlignedCell("Total Amount (Rs.)", invoiceData, totalAmountText, table);
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			addRightAlignedCell("Opening Balance", invoiceData, (invoiceData.getAdjustmentsMap().containsKey("OPENING-BALANCE")) ?
					invoiceData.getAdjustmentsMap().get("OPENING-BALANCE").getAmount().toString() :
					"0.00", table);
			addRightAlignedCell("Closing Balance", invoiceData, (invoiceData.getAdjustmentsMap().get("CLOSING-BALANCE") != null) ?
					invoiceData.getAdjustmentsMap().get("CLOSING-BALANCE").getAmount().toString() :
					"0.00", table);
		}
	}

	private void addRightAlignedCell(String cellText, InvoiceDataBean invoiceDataBean, String s, PdfPTable table) {
		PdfPCell cell = new PdfPCell(new Paragraph(cellText, COURIER));
		if (Objects.equals(invoiceDataBean.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		} else {
			cell.setHorizontalAlignment(Element.ALIGN_LEFT);
		}
		cell.setIndent(2);
		if (Objects.equals(invoiceDataBean.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			if (invoiceDataBean.getIsSrpStore() != null && invoiceDataBean.getIsSrpStore().equals(1)) {
				cell.setColspan(9);
			} else {
				cell.setColspan(8);
			}
		}
		table.addCell(cell);
		Paragraph p2 = new Paragraph();
		p2.setFont(TABLE_CONTENT_FONT);
		p2.add(s);
		table.addCell(p2);
	}

	private void addFooter(Document document, InvoiceDataBean invoiceData) throws DocumentException {
		Paragraph p2 = new Paragraph();
		leaveEmptyLine(p2, 3);
		p2.setAlignment(Element.ALIGN_LEFT);
		if (Objects.equals(invoiceData.getInvoiceType(), InvoiceType.FRANCHISE_INVOICE)) {
			p2.add(new Paragraph("Payment Terms: Prepaid Only", COURIER_SMALL_FOOTER));
		}
		document.add(p2);
		Paragraph p3 = new Paragraph();
		leaveEmptyLine(p3, 1);
		p3.setAlignment(Element.ALIGN_BASELINE);
		p3.add(new Paragraph("This is a computer generated " + convertToTitleCase(invoiceData.getInvoiceType().toString().replace("FRANCHISE_", "")) + ". Signature not required", COURIER_SMALL));
		document.add(p3);
	}

	private static void leaveEmptyLine(Paragraph paragraph, int number) {
		for (int i = 0; i < number; i++) {
			paragraph.add(new Paragraph(" "));
		}
	}

	private String getPdfNameWithDate(String s, String invoiceFileName) {
		return s + "/" + invoiceFileName + ".pdf";
	}

	@Override
	public void onEndPage(PdfWriter writer, Document document) {
		PdfContentByte canvas = writer.getDirectContent();
		Rectangle rect = document.getPageSize();
		rect.setBorder(Rectangle.BOX); // left, right, top, bottom border
		rect.setBorderWidth(5); // a width of 5 user units
		rect.setBorderColor(BaseColor.RED); // a red border
		rect.setUseVariableBorders(true); // the full width will be visible
		canvas.rectangle(rect);
	}

}
