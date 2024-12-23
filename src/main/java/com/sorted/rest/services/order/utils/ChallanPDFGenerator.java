package com.sorted.rest.services.order.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Table;
import com.sorted.rest.services.order.constants.FranchiseOrderConstants;
import org.springframework.stereotype.Component;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.services.order.beans.ChallanDataBean;
import com.sorted.rest.services.order.entity.FranchiseOrderItemEntity;

@Component
public class ChallanPDFGenerator extends PdfPageEventHelper {

	private AppLogger _LOGGER = LoggingManager.getLogger(ChallanPDFGenerator.class);

	private final static String REACHED_TIME_AT_STORE = "स्टोर पर पहोचने का समय:-";

	private final static String ARE_CRATES_RECEIVED_BY_STORE = "क्रेटस स्टोर ने उतरे है?:-";

	private final static String CRATES_RECEIVED_TIME = "क्रेटस उतरने का समय:-";

	private final static String CHECKING_TIME_AT_STORE = "स्टोर में चेकिंग शुरू करने का समय:-";

	private final static String STORE_DEPARTURE_TIME = "गाड़ी का स्टोर से निकलने का समय:-";

	private final static String VEHICLE_ARRIVAL_TIME_AT_WAREHOUSE = "गाड़ी का गोदाम में पहोचने का समय:-";

	private final static String STORE_OUTSTANDING_AMOUNT = "स्टोर की बकाया राशि:-";

	private final static Font COURIER_SMALL_FOOTER = new Font(Font.FontFamily.COURIER, 8, Font.BOLD);

	private final static Font DOCUMENT_HEADER = new Font(Font.FontFamily.COURIER, 10, Font.BOLD);

	private final static Font HEADER_CONTENT_FONT = new Font(Font.FontFamily.COURIER, 7, Font.BOLD);

	private final static Font TABLE_CONTENT_FONT = new Font(Font.FontFamily.COURIER, 7, Font.NORMAL, BaseColor.BLACK);

	private final static Font TABLE_CONTENT_FONT_HINDI = FontFactory.getFont("fonts/mangal.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 7, Font.NORMAL,
			BaseColor.BLACK);

	public synchronized void generatePdfReport(ChallanDataBean challanData) {
		_LOGGER.debug(String.format("generatePdfReport:: challan Data : %s", challanData));
		Document document = new Document();
		try {
			String directory = System.getProperty("user.dir");
			String challanFileName = challanData.getFileName();
			PdfWriter.getInstance(document, new FileOutputStream(getPdfName(directory, challanFileName)));
			document.open();
			addLogo(document);
			addDocTitle(document, challanData);
			addSkuDetails(challanData, document);
			document.close();
		} catch (DocumentException | IOException e) {
			_LOGGER.error("Error while generating challan Pdf Report", e);
			throw new ServerException(new ErrorBean(Errors.UPDATE_FAILED, "Error while generating challan Pdf Report", "challan"));
		}
	}

	private void addSkuDetails(ChallanDataBean challanData, Document document) throws DocumentException, IOException {
		int noOfColumns = 9;
		List<String> columnNames = new ArrayList<>();
		columnNames.add("SKU ID");
		columnNames.add("Product Description");
		columnNames.add("Crates Requested");
		columnNames.add("Crates Dispatched");
		columnNames.add("Qty Requested");
		columnNames.add("Actual Dispatch (weight)");
		if (challanData.getIsSrpStore() != null && challanData.getIsSrpStore().equals(1)) {
			columnNames.add("SRP");
		} else {
			columnNames.add("Rate");
		}
		columnNames.add("Amount");
		columnNames.add("Remarks");
		showSkuDetails(document, noOfColumns, columnNames, challanData);
	}

	private void addLogo(Document document) throws DocumentException {
		PdfPTable table = new PdfPTable(1);
		table.setWidthPercentage(100);
		table.getDefaultCell().setPadding(5);
		table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		table.getDefaultCell().setVerticalAlignment(Element.ALIGN_CENTER);

		Paragraph p = new Paragraph();
		p.setFont(DOCUMENT_HEADER);
		p.add("Challan");
		PdfPCell cell = new PdfPCell(p);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_CENTER);
		cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
		table.addCell(cell);
		p = new Paragraph();
		p.setFont(DOCUMENT_HEADER);
		p.add("BCFD Technologies Private limited");
		cell = new PdfPCell(p);
		cell.setHorizontalAlignment(Element.ALIGN_CENTER);
		cell.setVerticalAlignment(Element.ALIGN_CENTER);
		cell.setBackgroundColor(new BaseColor(175, 203, 107));
		table.addCell(cell);
		document.add(table);
	}

	private void addDocTitle(Document document, ChallanDataBean challanData) throws DocumentException {
		PdfPTable outerTable = new PdfPTable(2);
		PdfPCell cell;
		outerTable.setWidthPercentage(100);
		int[] columnWidths = { 40, 60 };
		outerTable.setWidths(columnWidths);
		outerTable.getDefaultCell().setBorder(0);
		outerTable.getDefaultCell().setPadding(5);
		outerTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		outerTable.getDefaultCell().setVerticalAlignment(Element.ALIGN_CENTER);
		orderDetails(outerTable, challanData);
		staticInfo(outerTable, challanData);
		cell = new PdfPCell();
		cell.setColspan(14);
		outerTable.addCell(cell);
		document.add(outerTable);
	}

	private static void staticInfo(PdfPTable table, ChallanDataBean challanData) throws DocumentException {
		PdfPTable staticTable = new PdfPTable(2);
		int[] columnWidths = { 70, 30 };
		staticTable.setWidths(columnWidths);
		staticTable.getDefaultCell().setPadding(3);
		staticTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
		staticTable.getDefaultCell().setVerticalAlignment(Element.ALIGN_CENTER);
		staticTable.addCell(new Paragraph(REACHED_TIME_AT_STORE, TABLE_CONTENT_FONT_HINDI));
		staticTable.addCell("");
		staticTable.addCell(new Paragraph(ARE_CRATES_RECEIVED_BY_STORE, TABLE_CONTENT_FONT_HINDI));
		staticTable.addCell(new Paragraph("yes   no", TABLE_CONTENT_FONT));
		staticTable.addCell(new Paragraph(CRATES_RECEIVED_TIME, TABLE_CONTENT_FONT_HINDI));
		staticTable.addCell("");
		staticTable.addCell(new Paragraph(CHECKING_TIME_AT_STORE, TABLE_CONTENT_FONT_HINDI));
		staticTable.addCell("");
		staticTable.addCell(new Paragraph(STORE_DEPARTURE_TIME, TABLE_CONTENT_FONT_HINDI));
		staticTable.addCell("");
		staticTable.addCell(new Paragraph(VEHICLE_ARRIVAL_TIME_AT_WAREHOUSE, TABLE_CONTENT_FONT_HINDI));
		staticTable.addCell("");
		staticTable.addCell(new Paragraph(STORE_OUTSTANDING_AMOUNT, TABLE_CONTENT_FONT_HINDI));
		staticTable.addCell(new Paragraph(challanData.getOutstandingAmount().toString(), TABLE_CONTENT_FONT));
		table.addCell(staticTable);
	}

	private void orderDetails(PdfPTable table, ChallanDataBean challanData) {
		PdfPTable dynamicTable = new PdfPTable(2);
		dynamicTable.getDefaultCell().setBorder(0);
		dynamicTable.addCell(new Paragraph("Name # :", TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph(challanData.getStoreName(), TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph("Store Id # :", TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph(challanData.getStoreId().toString(), TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph("Date :", TABLE_CONTENT_FONT));
		dynamicTable.addCell(new Paragraph(challanData.getDate().toString(), TABLE_CONTENT_FONT));
		if (challanData.getStoreData() != null) {
			dynamicTable.addCell(new Paragraph("Address :", TABLE_CONTENT_FONT));
			String storeAddress = "";
			if (challanData.getStoreData().getAddressLine1() != null) {
				storeAddress += challanData.getStoreData().getAddressLine1();
			}
			if (challanData.getStoreData().getAddressLine2() != null) {
				storeAddress += " " + challanData.getStoreData().getAddressLine2();
			}
			dynamicTable.addCell(new Paragraph(storeAddress, TABLE_CONTENT_FONT));
		}
		if (challanData.getOwnerDetails() != null) {
			dynamicTable.addCell(new Paragraph("Store Owner Number :", TABLE_CONTENT_FONT));
			dynamicTable.addCell(new Paragraph(challanData.getOwnerDetails().getPhoneNumber(), TABLE_CONTENT_FONT));
		}
		if (challanData.getAmDetails() != null) {
			dynamicTable.addCell(new Paragraph("AM Name :", TABLE_CONTENT_FONT));
			dynamicTable.addCell(new Paragraph(challanData.getAmDetails().getName(), TABLE_CONTENT_FONT));
			dynamicTable.addCell(new Paragraph("AM Number :", TABLE_CONTENT_FONT));
			dynamicTable.addCell(new Paragraph(challanData.getAmDetails().getPhoneNumber(), TABLE_CONTENT_FONT));
		}
		table.addCell(dynamicTable);
	}

	private void showSkuDetails(Document document, int noOfColumns, List<String> columnNames, ChallanDataBean challanData)
			throws DocumentException, IOException {
		Paragraph paragraph = new Paragraph();
		paragraph.setFont(COURIER_SMALL_FOOTER);
		leaveEmptyLine(paragraph, 3);
		int[] columnWidths = { 6, 30, 8, 8, 8, 8, 8, 8, 16 };
		PdfPTable table = new PdfPTable(noOfColumns);
		table.setTotalWidth(525);
		table.setWidths(columnWidths);
		table.setLockedWidth(true);
		for (int i = 0; i < noOfColumns; i++) {
			table.getDefaultCell().setPadding(1);
			Paragraph p = new Paragraph();
			p.setFont(HEADER_CONTENT_FONT);
			p.add(columnNames.get(i));
			table.addCell(p);
		}
		table.setHeaderRows(1);
		addSkuData(table, challanData);
		document.add(table);
	}

	// deliberately kept. to be used whenever required.
	private void createGap(Document document) throws DocumentException {
		Paragraph p = new Paragraph();
		leaveEmptyLine(p, 1);
		p.setAlignment(Element.ALIGN_LEFT);
		p.add(new Paragraph("", COURIER_SMALL_FOOTER));
		document.add(p);
	}

	private void addSkuData(PdfPTable table, ChallanDataBean challanData) {
		for (FranchiseOrderItemEntity franchiseOrderItem : challanData.getOrderItems()) {
			table.setWidthPercentage(100);
			table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_LEFT);
			table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.getDefaultCell().setPadding(1);
			table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getSkuCode(), TABLE_CONTENT_FONT)));
			table.addCell(new PdfPCell(getBilingualParagraph(franchiseOrderItem.getProductName())));
			table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getOrderedCrateQty().toString(), TABLE_CONTENT_FONT)));
			if (franchiseOrderItem.getStatus() == FranchiseOrderConstants.FranchiseOrderItemStatus.PACKED || franchiseOrderItem.getStatus() == FranchiseOrderConstants.FranchiseOrderItemStatus.NOT_AVAILABLE) {
				table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getCratesPicked().toString(), TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getOrderedQty().toString(), TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getWeightPicked().toString(), TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getSalePrice().toString(), TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getSpGrossAmount().toString(), TABLE_CONTENT_FONT)));
				if (franchiseOrderItem.getMetadata() != null && franchiseOrderItem.getMetadata().getDeliveryNumber() != null && franchiseOrderItem.getMetadata()
						.getDeliveryNumber() > 1) {
					table.addCell(new PdfPCell(
							new Paragraph(String.format("D %s delivery", franchiseOrderItem.getMetadata().getDeliveryNumber()), TABLE_CONTENT_FONT)));
				} else {
					table.addCell(new PdfPCell(new Paragraph("", TABLE_CONTENT_FONT)));
				}
			} else {
				table.addCell(new PdfPCell(new Paragraph("", TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph(franchiseOrderItem.getOrderedQty().toString(), TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph("", TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph("", TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph("", TABLE_CONTENT_FONT)));
				table.addCell(new PdfPCell(new Paragraph("On The Way", TABLE_CONTENT_FONT)));
			}
		}
		if (challanData.getOrderStatus() == FranchiseOrderConstants.FranchiseOrderStatus.ORDER_BILLED) {
			addColumnPadding(table,6);
			table.addCell(new PdfPCell(new Paragraph("Total Amount:", HEADER_CONTENT_FONT)));
			table.addCell(new PdfPCell(new Paragraph(challanData.getTotalSpGrossAmount().toString(), HEADER_CONTENT_FONT)));
			table.addCell(new PdfPCell(new Paragraph("", TABLE_CONTENT_FONT)));
			addColumnPadding(table, 6);
			table.addCell(new PdfPCell(new Paragraph("Discount", HEADER_CONTENT_FONT)));
			table.addCell(new PdfPCell(new Paragraph(challanData.getOfferDiscountAmount().toString(), HEADER_CONTENT_FONT)));
			table.addCell(new PdfPCell(new Paragraph("", TABLE_CONTENT_FONT)));
			addColumnPadding(table, 6);
			table.addCell(new PdfPCell(new Paragraph("Net Amount:", HEADER_CONTENT_FONT)));
			table.addCell(new PdfPCell(new Paragraph(challanData.getFinalBillAmount().toString(), HEADER_CONTENT_FONT)));
			table.addCell(new PdfPCell(new Paragraph("", TABLE_CONTENT_FONT)));
		}
	}

	private void addColumnPadding(PdfPTable table, int paddingNumber) {
		for (int i = 0; i < paddingNumber; i++) {
			table.addCell(new PdfPCell(new Paragraph("", TABLE_CONTENT_FONT))).setBorder(0);
		}
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

	private void leaveEmptyLine(Paragraph paragraph, int number) {
		for (int i = 0; i < number; i++) {
			paragraph.add(new Paragraph(" "));
		}
	}

	private String getPdfName(String directory, String invoiceFileName) {
		return directory + "/" + invoiceFileName;
	}

	@Override
	public void onEndPage(PdfWriter writer, Document document) {
		PdfContentByte canvas = writer.getDirectContent();
		Rectangle rect = document.getPageSize();
		rect.setBorder(Rectangle.BOX);
		rect.setBorderWidth(5);
		rect.setBorderColor(BaseColor.RED);
		rect.setUseVariableBorders(true);
		canvas.rectangle(rect);
	}
}
