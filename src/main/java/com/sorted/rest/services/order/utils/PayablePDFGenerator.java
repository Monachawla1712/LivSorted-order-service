package com.sorted.rest.services.order.utils;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.utils.CollectionUtils;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.services.order.beans.UserServiceResponse;
import com.sorted.rest.services.order.beans.WalletBean;
import com.sorted.rest.services.order.entity.OrderEntity;
import com.sorted.rest.services.order.entity.OrderItemEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PayablePDFGenerator extends PdfPageEventHelper {

	private AppLogger _LOGGER = LoggingManager.getLogger(PayablePDFGenerator.class);

	private static final Font DOCUMENT_HEADER_2 = new Font(Font.FontFamily.HELVETICA, 14, Font.NORMAL);

	private static final Font HEADER_CONTENT_FONT_2 = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK);

	private static final Font DISCOUNT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.ITALIC, BaseColor.GRAY);


	public void sortOrderItems(OrderEntity cart) {
		if (CollectionUtils.isNotEmpty(cart.getOrderItems())) {
			cart.getOrderItems().forEach(item -> {
				if (item.getFinalQuantity() == 0d) {
					item.getMetadata().setIsShowOnOrderSummary(Boolean.FALSE);
				} else {
					item.getMetadata().setIsShowOnOrderSummary(Boolean.TRUE);
				}
			});
			cart.getOrderItems().sort(Comparator.comparing((OrderItemEntity item) -> item.getMetadata().getIsOrderOfferItem())
					.thenComparing(OrderItemEntity::getCreatedAt));
		}
	}

	public synchronized void generatePdfReport(OrderEntity orderEntity, WalletBean wallet, UserServiceResponse userDetails) {
		Document document = new Document(PageSize.A4);
		try {
			String s = System.getProperty("user.dir");
			String fileName = orderEntity.getDisplayOrderId();
			PdfWriter.getInstance(document, new FileOutputStream(getPdfNameWithDate(s, fileName)));
			document.open();
			preparePdf(document, orderEntity, wallet, userDetails);
			document.close();
		} catch (FileNotFoundException | DocumentException e) {
			_LOGGER.error("FileNotFoundException | DocumentException  in generatePdfReport invoice:  ", e);
		} catch (IOException e) {
			_LOGGER.error("IOException in generatePdfReport:", e);
			throw new RuntimeException(e);
		}

	}

	private String getPdfNameWithDate(String s, String fileName) {
		return s + "/" + fileName + ".pdf";
	}

	private void preparePdf(Document document, OrderEntity orderEntity, WalletBean wallet, UserServiceResponse userDetails)
			throws DocumentException, IOException {
		Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
		Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
		addCompanyDetailsAndQrCode(document, userDetails, orderEntity);
		addCustomerDetails(document, orderEntity);
		Map<String, BigDecimal> values = addOrderItems(document, orderEntity, normalFont, boldFont);
		PdfPTable table = new PdfPTable(new float[] { 5, 1, 3 }); // Create a table with 3 columns and set their widths
		table.setWidthPercentage(100);
		addItemTotal(table, orderEntity, values.get("totalVolumeDiscount"), values.get("totalPromoDiscount"), values.get("totalFreeComboOfferDiscount"));
//		addFreeItemTotal(table, orderEntity);
		addExtraFees(table, orderEntity);
		addBillTotal(table, orderEntity);
		if (wallet != null) {
			addPreviousDues(table, orderEntity, wallet);
			document.add(table);
			addDottedLine(document);
			Double totalPayable = Double.valueOf(wallet.getAmount()).compareTo(0d) < 0 ? wallet.getAmount() : 0;
			addTotalPayable(document, totalPayable);
			addDottedLine(document);
		} else {
			document.add(table);
			addDottedLine(document);
		}
		addCashBack(document, values.get("totalCashBack"));
		if(orderEntity.getTotalDiscountAmount() > 0) {
			addSavingsParagraph(document, orderEntity.getTotalDiscountAmount() + values.get("totalCashBack").doubleValue());
		}
	}

	private void addSavingsParagraph(Document document, Double savingsAmount) throws DocumentException {
		Font font = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
		Paragraph p = new Paragraph();
		Chunk congratsLabel = new Chunk("\n\nCongratulations!", font);
		Chunk savings = new Chunk("\n\nYou saved Rs " + Integer.valueOf(savingsAmount.intValue()).toString() + " on this order", font);
		p.setAlignment(Element.ALIGN_CENTER);
		p.add(congratsLabel);
		p.add(savings);
		document.add(p);
	}

	private void addCompanyDetailsAndQrCode(Document document, UserServiceResponse userDetails, OrderEntity orderEntity) throws DocumentException, IOException {
		// Create a PdfPTable with 2 columns
		PdfPTable table = new PdfPTable(2);
		table.setWidthPercentage(90);
		Image logoImage = Image.getInstance(new URL("https://files-sorted-prod.s3.ap-south-1.amazonaws.com/public/handpikd.png"));
		logoImage.scaleAbsolute(100, 60);
		PdfPCell textCell = new PdfPCell();
		textCell.addElement(logoImage);
//		textCell.addElement(new Phrase("handpickd", DOCUMENT_HEADER));
		String companyDetails = ParamsUtils.getParam("COMPANY_DETAILS");
		List<String> companyDetailsArray = List.of(companyDetails.split("\\|"));
		textCell.addElement(new Phrase(companyDetailsArray.get(0), DOCUMENT_HEADER_2));
		textCell.addElement(new Phrase(companyDetailsArray.get(1), DOCUMENT_HEADER_2));
		textCell.setBorder(Rectangle.NO_BORDER);
		table.addCell(textCell);

		// TODO -- Commented out the QR, remove empty cell and comments to get QR

		// Check if the QR code URL is not empty
		if (userDetails != null && StringUtils.isNotEmpty(userDetails.getEasebuzzQrCode())) {
			// Create an Image instance from the QR code URL
//			Image image = Image.getInstance(new URL(userDetails.getEasebuzzQrCode()));
//			image.scaleAbsolute(140, 140); // Set the image size

			// Create a PdfPCell for the image
//			PdfPCell imageCell = new PdfPCell(image);
			// imageCell.addElement(new Phrase("\nScan To Pay", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK)));
//			imageCell.setBorder(Rectangle.NO_BORDER);
//			imageCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			PdfPCell emptyCell = new PdfPCell(new Phrase(""));
			emptyCell.setBorder(Rectangle.NO_BORDER);
			table.addCell(emptyCell);
//			PdfPCell scanToPayCell = new PdfPCell(new Phrase("Scan To Pay\t", FontFactory.getFont(FontFactory.HELVETICA, 12, Font.NORMAL, BaseColor.BLACK)));
//			scanToPayCell.setBorder(Rectangle.NO_BORDER);
//			scanToPayCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
			table.addCell(emptyCell);

		} else {
			// Add an empty cell if the QR code URL is empty
			PdfPCell emptyCell = new PdfPCell(new Phrase(""));
			emptyCell.setBorder(Rectangle.NO_BORDER);
			table.addCell(emptyCell);

			PdfPCell emptyCell2 = new PdfPCell(new Phrase(""));
			emptyCell2.setBorder(Rectangle.NO_BORDER);
			table.addCell(emptyCell2);
		}
		table.setHorizontalAlignment(Element.ALIGN_LEFT);
		// Add the table to the document
		document.add(table);
		addDottedLine(document);

		// Close the document
	}

	private void addCustomerDetails(Document document, OrderEntity orderEntity) throws DocumentException {
		String name = orderEntity.getMetadata().getContactDetail().getName() != null ? orderEntity.getMetadata().getContactDetail().getName() : "";
		document.add(new Paragraph("Customer: ".concat(name), HEADER_CONTENT_FONT_2));
		document.add(new Paragraph("Order: ".concat(orderEntity.getDisplayOrderId()), HEADER_CONTENT_FONT_2));
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date deliveryDate = orderEntity.getDeliveryDate();
		String formattedDate = dateFormat.format(deliveryDate);
		document.add(new Paragraph("Delivery Date: " + formattedDate, HEADER_CONTENT_FONT_2));
		addDottedLine(document);
	}

	private void addDottedLine(Document document) throws DocumentException {
		document.add(new Paragraph("----------------------------------------------------------------------------------------------------------------------"));
	}

	private Map<String, BigDecimal> addOrderItems(Document document, OrderEntity orderEntity, Font normalFont, Font boldFont) throws DocumentException {
		PdfPTable table = new PdfPTable(new float[] { 1, 5, 3 }); // Create a table with 3 columns and set their widths
		table.setWidthPercentage(100);
		PdfPCell cell1 = new PdfPCell(new Paragraph("", boldFont));
		PdfPCell cell2 = new PdfPCell(new Paragraph("ITEM", boldFont));
		PdfPCell cell3 = new PdfPCell(new Paragraph("AMOUNT", boldFont));
		cell1.setBorderWidth(0);
		cell2.setBorderWidth(0);
		cell3.setBorderWidth(0);
		table.addCell(cell1);
		table.addCell(cell2);
		table.addCell(cell3);
		table.addCell(createEmptyCell(3));
		int i = 1;
		BigDecimal totalCashBack = BigDecimal.ZERO;
		BigDecimal totalVolumeDiscount = BigDecimal.ZERO;
		BigDecimal totalPromoDiscount = BigDecimal.ZERO;
		BigDecimal totalFreeComboOfferDiscount = BigDecimal.ZERO;
		Map<String, BigDecimal> values = new HashMap<>();
		sortOrderItems(orderEntity);
		for (OrderItemEntity item : orderEntity.getOrderItems()) {
			if (item.getMetadata().getIsShowOnOrderSummary() != null && !item.getMetadata().getIsShowOnOrderSummary()) {
				continue;
			}
			if (item.getMetadata().getIsCashbackItem()) {
				totalCashBack = totalCashBack.add(BigDecimal.valueOf(item.getFinalAmount()));
			}
			if(hasDiscountPercentage(item)) {
				BigDecimal volumeDiscount = BigDecimal.valueOf(item.getDiscountAmount());
				totalVolumeDiscount = totalVolumeDiscount.add(volumeDiscount).setScale(0, RoundingMode.HALF_UP);
			}
			if(isOrderOfferItem(item)) {
				BigDecimal comboOfferDiscount = BigDecimal.valueOf(item.getDiscountAmount());
				totalFreeComboOfferDiscount = totalFreeComboOfferDiscount.add(comboOfferDiscount).setScale(0, RoundingMode.HALF_UP);
			}
			if(!hasDiscountPercentage(item) && !isOrderOfferItem(item)) {
				totalPromoDiscount = totalPromoDiscount.add(BigDecimal.valueOf(item.getDiscountAmount()).setScale(0, RoundingMode.HALF_UP));
			}
			PdfPCell cell = new PdfPCell(new Paragraph(String.valueOf(i++)));
			cell.setBorderWidth(0);
			table.addCell(cell);
			Phrase itemPart1 = new Phrase(item.getProductName(), FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, BaseColor.BLACK));
			BigDecimal price = item.getMarkedPrice();
			Phrase itemPart2 = new Phrase(String.format("\n%s/%s\n", price, getUOMFromString(item.getUom())), normalFont);
			Phrase itemPhrase = new Phrase();
			itemPhrase.add(itemPart1);
			itemPhrase.add(itemPart2);
			String finalAmount = item.getMrpGrossAmount().toString();
			if (item.getMrpGrossAmount() == 0) {
				if (item.getFinalQuantity() == 0) {
					finalAmount = "NA";
				} else {
					finalAmount = "Free";
				}
			}
			Phrase itemPart3 = new Phrase(String.format("Rs %s", finalAmount), FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD, BaseColor.BLACK));
			String uom = getUom(item.getUom());
			if (uom.equals("Kg") && item.getFinalQuantity().compareTo(1d) < 0) {
				uom = "gms";
			}
			Phrase itemPart4 = new Phrase(String.format("\n%s %s", selectedQuantityDisplay(item.getFinalQuantity(), getUOMFromString(item.getUom())), uom),
					normalFont);
			Phrase itemPhrase2 = new Phrase();
			itemPhrase2.add(itemPart3);
			itemPhrase2.add(itemPart4);
			addItemDiscounts(itemPhrase, itemPhrase2, item);
			PdfPCell productCell = new PdfPCell(itemPhrase);
			productCell.setBorderWidth(0);
			table.addCell(productCell);
			PdfPCell priceCell = new PdfPCell(itemPhrase2);
			priceCell.setBorderWidth(0);
			table.addCell(priceCell);
			table.addCell(createEmptyCell(3));
		}
		values.put("totalCashBack", totalCashBack);
		values.put("totalPromoDiscount", totalPromoDiscount);
		values.put("totalFreeComboOfferDiscount", totalFreeComboOfferDiscount);
		values.put("totalVolumeDiscount", totalVolumeDiscount);
		document.add(table);
		addDottedLine(document);
		return values;
	}

	private void addItemDiscounts(Phrase labelPhrase, Phrase amountPhrase, OrderItemEntity item) {
		String discountLabel = null;
		BigDecimal discount = null;
		if (hasDiscountPercentage(item)) {
			discountLabel = "Volume Discount " + item.getMetadata().getDiscountPercentage() + "%";
			discount = BigDecimal.valueOf(item.getDiscountAmount());
		}
		else if (isOrderOfferItem(item)) {
			discountLabel = "Free Combo Offer";
			discount = BigDecimal.valueOf(item.getDiscountAmount());
		}
		else if (item.getDiscountAmount().compareTo(0d) > 0) {
			discountLabel = "Promo Discount";
			discount = BigDecimal.valueOf(item.getDiscountAmount());
		}
		if(discountLabel != null) {
			labelPhrase.add(new Phrase(discountLabel, DISCOUNT_FONT));
			amountPhrase.add(new Phrase("\n-Rs ".concat(discount.toString()), DISCOUNT_FONT));
		}
	}

	private PdfPCell createEmptyCell(int colspan) {
		PdfPCell cell = new PdfPCell();
		cell.setColspan(colspan);
		cell.setFixedHeight(10); // Set the height for line spacing, adjust as needed
		cell.setBorder(Rectangle.NO_BORDER); // No border for empty cell
		return cell;
	}

//	private void addCashBack(Document document, BigDecimal totalCashBack) throws DocumentException {
//		if (totalCashBack.compareTo(BigDecimal.ZERO) != 0) {
//			Paragraph paragraph = new Paragraph(String.format(
//					"Congratulations! Your cashback Rs %s earned from Gift Tree will be deposited into your \nwallet within 24 hours after successful delivery and payment of this order.",
//					totalCashBack));
//			document.add(paragraph);
//			// addDottedLine(document);
//		}
//	}

	private void addCashBack(Document document, BigDecimal totalCashBack) throws DocumentException {
		if (totalCashBack.compareTo(BigDecimal.ZERO) != 0) {
			PdfPCell cell1 = new PdfPCell(new Paragraph("Total cashbacks", FontFactory.getFont(FontFactory.HELVETICA, 12)));
			PdfPCell cell3 = new PdfPCell(new Paragraph(String.format("Rs %s", totalCashBack)));
			PdfPTable table = new PdfPTable(new float[] { 5, 1, 3 });
			table.setWidthPercentage(100);
			cell1.setBorder(Rectangle.NO_BORDER);
			cell3.setBorder(Rectangle.NO_BORDER);
			table.addCell(cell1);
			table.addCell(createEmptyCell(1));
			table.addCell(cell3);
			document.add(table);
		}
	}

	private void addPreviousDues(PdfPTable table, OrderEntity orderEntity, WalletBean wallet) throws DocumentException {
		Integer balance = BigDecimal.valueOf(wallet.getAmount()).add(BigDecimal.valueOf(orderEntity.getFinalBillAmount())).intValue();
		String displayString;
		if (balance.compareTo(0) >= 0) {
			displayString = "Balance";
		} else {
			displayString = "Previous Dues";
		}

		PdfPCell cell1 = new PdfPCell(new Paragraph(displayString, FontFactory.getFont(FontFactory.HELVETICA, 12)));
		PdfPCell cell3 = new PdfPCell(new Paragraph(String.format("Rs %s", Math.abs(balance))));
		cell1.setBorderWidth(0);
		cell3.setBorderWidth(0);
		table.addCell(cell1);
		table.addCell(createEmptyCell(1));
		table.addCell(cell3);

	}

	private void addTotalPayable(Document document, Double totalPayable) throws DocumentException {
		PdfPTable table = new PdfPTable(new float[] { 5, 1, 3 });
		table.setWidthPercentage(100);
		Paragraph totalPayableText = new Paragraph("Total Payable ", FontFactory.getFont(FontFactory.HELVETICA, 12));
		Paragraph amount = new Paragraph(String.format("Rs %s", Math.abs(totalPayable.intValue())), FontFactory.getFont(FontFactory.HELVETICA, 20));
		PdfPCell cell1 = new PdfPCell(totalPayableText);
		PdfPCell cell2 = new PdfPCell(amount);
		cell1.setBorderWidth(0);
		cell2.setBorderWidth(0);
		table.addCell(cell1);
		table.addCell(createEmptyCell(1));
		table.addCell(cell2);
		document.add(table);
	}

	private void addItemTotal(PdfPTable table, OrderEntity orderEntity, BigDecimal totalVolumeDiscount, BigDecimal totalPromoDiscount,
			BigDecimal totalFreeComboOfferDiscount) throws DocumentException {
		PdfPCell cell1 = new PdfPCell(new Paragraph("Items Total", FontFactory.getFont(FontFactory.HELVETICA, 12)));
		PdfPCell cell3 = new PdfPCell(new Paragraph(String.format("Rs %s", orderEntity.getTotalMrpGrossAmount())));
		cell1.setBorderWidth(0);
		cell3.setBorderWidth(0);
		table.addCell(cell1);
		table.addCell(createEmptyCell(1));
		table.addCell(cell3);
		addDiscountTotalCells(table, totalVolumeDiscount, "Total Volume Discount");
		addDiscountTotalCells(table, totalPromoDiscount, "Total Promo Discount");
		addDiscountTotalCells(table, totalFreeComboOfferDiscount, "Total Free Combo Offer");
	}

	private void addExtraFees(PdfPTable table, OrderEntity orderEntity) {
		if(orderEntity != null && orderEntity.getExtraFeeDetails() != null && orderEntity.getExtraFeeDetails().getOzoneWashingCharge() > 0) {
			String pestFreeFeesLabel = ParamsUtils.getParam("PEST_FREE_FEES_LABEL");
			PdfPCell cell1 = new PdfPCell(new Paragraph(pestFreeFeesLabel, FontFactory.getFont(FontFactory.HELVETICA, 12)));
			PdfPCell cell3 = new PdfPCell(new Paragraph(String.format("Rs %s", orderEntity.getExtraFeeDetails().getOzoneWashingCharge().intValue())));
			cell1.setBorderWidth(0);
			cell3.setBorderWidth(0);
			table.addCell(cell1);
			table.addCell(createEmptyCell(1));
			table.addCell(cell3);
		}
	}

	private void addDiscountTotalCells(PdfPTable table, BigDecimal discount, String label) {
		if(discount.compareTo(BigDecimal.ZERO) != 0) {
			PdfPCell discountCell = new PdfPCell(new Paragraph(label, DISCOUNT_FONT));
			PdfPCell amountCell = new PdfPCell(new Paragraph(String.format("-Rs %s", discount), DISCOUNT_FONT));
			discountCell.setBorderWidth(0);
			amountCell.setBorderWidth(0);
			table.addCell(discountCell);
			table.addCell(createEmptyCell(1));
			table.addCell(amountCell);
		}
	}

//	private void addFreeItemTotal(PdfPTable table, OrderEntity orderEntity) throws DocumentException {
//		if (orderEntity.getTotalDiscountAmount().compareTo(0d) != 0) {
//			PdfPCell cell1 = new PdfPCell(new Paragraph("Free Items Total", FontFactory.getFont(FontFactory.HELVETICA, 12)));
//			PdfPCell cell3 = new PdfPCell(new Paragraph(String.format("Rs %s", orderEntity.getTotalDiscountAmount().intValue())));
//			cell1.setBorderWidth(0);
//			cell3.setBorderWidth(0);
//			table.addCell(cell1);
//			table.addCell(createEmptyCell(1));
//			table.addCell(cell3);
//		}
//	}

	private void addBillTotal(PdfPTable table, OrderEntity orderEntity) throws DocumentException {
		PdfPCell cell1 = new PdfPCell(new Paragraph("Bill Total", FontFactory.getFont(FontFactory.HELVETICA, 16)));
		PdfPCell cell3 = new PdfPCell(new Paragraph(String.format("Rs %s", orderEntity.getFinalBillAmount().intValue()),  FontFactory.getFont(FontFactory.HELVETICA, 16)));
		cell1.setBorderWidth(0);
		cell3.setBorderWidth(0);
		table.addCell(cell1);
		table.addCell(createEmptyCell(1));
		table.addCell(cell3);
	}

	private String getUom(String uom) {
		switch (uom.toUpperCase()) {
		case "GRAM":
			return "gm";
		case "KILOGRAM":
			return "Kg";
		case "PIECE":
		case "NUMBER":
			return "Pcs";
		case "PACKET":
			return "Pkt";
		case "BUNDLE":
			return "Bundle";
		default:
			return uom;
		}
	}

	private String getUOMFromString(String uom) {
		switch (uom.toUpperCase()) {
		case "GRAM":
			return "gm";
		case "KILOGRAM":
			return "Kg";
		case "PIECE":
		case "NUMBER":
			return "Piece";
		case "PACKET":
			return "Packet";
		case "BUNDLE":
			return "Bundle";
		default:
			return uom;
		}
	}

	private String selectedQuantityDisplay(Double qty, String uom) {
		BigDecimal bdQty = BigDecimal.valueOf(qty);
		if (qty.compareTo(1d) < 0 && uom == "Kg") {
			BigDecimal finalQuantity = bdQty.multiply(BigDecimal.valueOf(1000d));
			return finalQuantity.setScale(2, RoundingMode.HALF_UP).toPlainString();
		} else if (uom.equalsIgnoreCase("Piece") || uom.equalsIgnoreCase("Packet") || uom.equalsIgnoreCase("Bundle")) {
			return String.valueOf(bdQty.intValue());
		}
		return bdQty.setScale(2, RoundingMode.HALF_UP).toPlainString();
	}

	private boolean hasDiscountPercentage(OrderItemEntity item) {
		return item.getMetadata().getDiscountPercentage() != null && item.getMetadata().getDiscountPercentage() > 0;
	}

	private boolean isOrderOfferItem(OrderItemEntity item) {
		return item.getMetadata().getIsOrderOfferItem() != null && item.getMetadata().getIsOrderOfferItem() && item.getDiscountAmount() > 0;
	}
}
