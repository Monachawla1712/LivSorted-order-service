package com.sorted.rest.services.order.services;

import com.sorted.rest.common.beans.ErrorBean;
import com.sorted.rest.common.exceptions.ServerException;
import com.sorted.rest.common.logging.AppLogger;
import com.sorted.rest.common.logging.LoggingManager;
import com.sorted.rest.common.properties.Errors;
import com.sorted.rest.common.utils.ParamsUtils;
import com.sorted.rest.common.utils.SessionUtils;
import com.sorted.rest.services.common.upload.AWSUploadService;
import com.sorted.rest.services.order.clients.ClientService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.nio.file.Path;

@Service
public class ZIPService {

	private AppLogger _LOGGER = LoggingManager.getLogger(ZIPService.class);

	@Autowired
	private AWSUploadService awsUploadService;

	@Autowired
	private ClientService clientService;

	private String createDirectory(Date date) {
		File directoryPath = new File(System.getProperty("user.dir"));
		String folder = date.toString().concat("-").concat(String.valueOf(Instant.now().toEpochMilli()));
		String directory = directoryPath + File.separator + folder;
		new File(directory).mkdirs();
		return directory;
	}

	private void zipDirectory(File zipDirectory, String zipFilename) throws IOException {
		ZipOutputStream zout = null;
		FileOutputStream fout = null;
		File zipFile = new File(zipFilename);
		try {
			fout = new FileOutputStream(zipFile);
			zout = new ZipOutputStream(fout);
			String path = zipDirectory.getName() + File.separator;
			zout.putNextEntry(new ZipEntry(path));
			zipSubDirectory(path, zipDirectory, zout);
			zout.closeEntry();
		} finally {
			IOUtils.closeQuietly(zout);
			IOUtils.closeQuietly(fout);
		}
	}

	private void zipSubDirectory(String basePath, File dir, ZipOutputStream zout) throws IOException {
		byte[] buffer = new byte[4096];
		File[] files = Optional.of(dir.listFiles()).orElse(new File[0]);
		for (File file : files) {
			if (file.isDirectory()) {
				String path = FilenameUtils.concat(basePath, file.getName()) + File.separator;
				zout.putNextEntry(new ZipEntry(path));
				zipSubDirectory(path, file, zout);
				zout.closeEntry();
			} else {
				FileInputStream fin = null;
				try {
					fin = new FileInputStream(file);
					String path = FilenameUtils.concat(basePath, file.getName());
					zout.putNextEntry(new ZipEntry(path));
					int length;
					while ((length = fin.read(buffer)) > 0) {
						zout.write(buffer, 0, length);
					}
					zout.closeEntry();
				} finally {
					IOUtils.closeQuietly(fin);
				}
			}
		}
	}

	public ByteArrayResource generateZipFile(List<String> urls, Date date) {
		ByteArrayResource resource = null;
		try {
			String directory = createDirectory(date);
			_LOGGER.info(String.format("invoice zip creation started at: %s", new Date()));
			for (String url : urls) {
				Integer index = url.lastIndexOf(File.separator);
				File fileLoc = new File(directory + File.separator + url.substring(index + 1));
				downloadResourceFile(url, fileLoc);
			}
			_LOGGER.info(String.format("invoice zip creation ended at: %s", new Date()));
			zipDirectory(new File(directory), directory.substring(directory.lastIndexOf(File.separator) + 1).concat(".zip"));
			resource = readResourceFile(directory,date);
		} catch (IOException err) {
			_LOGGER.error("Error while generating ZipFile", err);
			throw new ServerException(new ErrorBean(Errors.UPDATE_FAILED, String.format("Error while generating ZipFile", err.getMessage()), "invoice"));
		}
		return resource;
	}

	private ByteArrayResource readResourceFile(String directory,Date date) throws IOException {
		File fileLoc = new File(directory.concat(".zip"));
		Path path = Paths.get(fileLoc.getAbsolutePath());
		ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
		String uploadFileName = generateUploadFileName(date);
		String fileUrl = uploadZipFileToAWS(uploadFileName);
		if (fileUrl != null) {
			sendInvoiceDownloadLinkOverMail(fileUrl, SessionUtils.getAuthUserId());
		}
		deleteCreatedFile(directory);
		return resource;
	}

	private void sendInvoiceDownloadLinkOverMail(String fileUrl,UUID userId) {
		try {
			String downloadLink = ParamsUtils.getParam("CLOUDFRONT_URL").concat("/").concat(fileUrl);
			clientService.sendZipFileLink(downloadLink,userId);
		} catch (Exception e) {
			_LOGGER.error("Error while sending Email");
		}
	}

	private String generateUploadFileName(Date date) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
		String formattedDate = formatter.format(date);
		return ("invoice-zip").concat("-").concat(formattedDate).concat(".zip");
	}

	private String uploadZipFileToAWS(String fileName) {
		try {
			String bucketName = ParamsUtils.getParam("SORTED_FILES_BUCKET_NAME");
			String subDirectory = ParamsUtils.getParam("INVOICE_FILES_SUBDIRECTORY");
			File directoryPath = new File(System.getProperty("user.dir"));
			File[] files = directoryPath.listFiles();
			for (File file : files) {
				if (file.getName().endsWith(".zip")) {
					byte[] fileBytes = Files.readAllBytes(Path.of(directoryPath.getPath().concat("/").concat(file.getName())));
					Object response = awsUploadService.uploadFile(bucketName, subDirectory, fileBytes, fileName);
					return response.toString();
				}
			}
		} catch (Exception e) {
			_LOGGER.error("Error while uploading Zip File", e);
		}
		return null;
	}

	private void deleteCreatedFile(String directory) {
		File directoryPath = new File(System.getProperty("user.dir"));
		String folderName = directory.substring(directory.lastIndexOf(File.separator) + 1);
		File files[] = directoryPath.listFiles();
		for (File file : files) {
			if (file.getName().endsWith(".zip")) {
				file.delete();
			} else if (file.getName().equals(folderName)) {
				for (File subfile : file.listFiles()) {
					subfile.delete();
				}
				file.delete();
			}
		}
	}

	private void downloadResourceFile(String urlLink, File fileLoc) throws IOException {
		byte[] buffer = new byte[1024];
		int readbyte = 0;
		URL url = new URL(urlLink);
		HttpURLConnection http = (HttpURLConnection) url.openConnection();
		BufferedInputStream input = new BufferedInputStream(http.getInputStream());
		FileOutputStream ouputfile = new FileOutputStream(fileLoc);
		BufferedOutputStream bufferOut = new BufferedOutputStream(ouputfile, 1024);
		while ((readbyte = input.read(buffer, 0, 1024)) >= 0) {
			bufferOut.write(buffer, 0, readbyte);
		}
		bufferOut.close();
		input.close();
	}
}
