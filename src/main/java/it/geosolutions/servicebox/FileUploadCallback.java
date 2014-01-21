/*
 *  ServiceBox
 *  Copyright (C) 2014 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.servicebox;

import it.geosolutions.servicebox.utils.Utilities;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * File upload callback. Limit the files to be uploaded
 * 
 * @author adiaz
 * 
 */
public class FileUploadCallback implements Serializable, Callback {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8442796981472140058L;

	private final static Logger LOGGER = Logger
			.getLogger(FileUploadCallback.class.toString());

	private FileUploadCallbackConfiguration callbackConfiguration;

	/**
	 * Handle a GET request
	 * 
	 * @param request
	 * @param response
	 * 
	 * @return CallbackResult
	 * 
	 * @throws IOException
	 */
	public ServiceBoxActionParameters onGet(HttpServletRequest request,
			HttpServletResponse response,
			ServiceBoxActionParameters callbackResult) throws IOException {
		if (callbackResult == null) {
			callbackResult = new ServiceBoxActionParameters();
		}
		callbackResult.setSuccess(true);
		return callbackResult;
	}

	/**
	 * Handle a POST request
	 * 
	 * @param request
	 * @param response
	 * 
	 * @return CallbackResult
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public ServiceBoxActionParameters onPost(HttpServletRequest request,
			HttpServletResponse response,
			ServiceBoxActionParameters callbackResult) throws IOException {

		// Get items if already initialized
		List<FileItem> items = null;
		if (callbackResult == null) {
			callbackResult = new ServiceBoxActionParameters();
		} else {
			items = callbackResult.getItems();
		}

		String temp = callbackConfiguration.getTempFolder();
		int buffSize = callbackConfiguration.getBuffSize();
		int itemSize = 0;
		long maxSize = 0;
		String itemName = null;

		File tempDir = new File(temp);

		try {
			if (items == null && ServletFileUpload.isMultipartContent(request)
					&& tempDir != null && tempDir.exists()) {
				// items are not initialized. Read it from the request

				DiskFileItemFactory fileItemFactory = new DiskFileItemFactory();

				/*
				 * Set the size threshold, above which content will be stored on
				 * disk.
				 */
				fileItemFactory.setSizeThreshold(buffSize); // 1 MB

				/*
				 * Set the temporary directory to store the uploaded files of
				 * size above threshold.
				 */
				fileItemFactory.setRepository(tempDir);

				ServletFileUpload uploadHandler = new ServletFileUpload(
						fileItemFactory);

				/*
				 * Parse the request
				 */
				items = uploadHandler.parseRequest(request);

			}

			// Read items
			if (items != null) {

				itemSize = items.size();
				callbackResult.setItems(items);
				if (itemSize < this.callbackConfiguration.getMaxItems()) {
					// only if item size not exceeded max
					for (FileItem item : items) {
						if (item.getSize() > maxSize) {
							maxSize = item.getSize();
							itemName = item.getName();
							if (maxSize > this.callbackConfiguration
									.getMaxSize()) {
								// max size exceeded
								break;
							}
						}
					}
				}
			} else {
				itemSize = 1;
				maxSize = request.getContentLength();
			}

		} catch (Exception ex) {
			if (LOGGER.isLoggable(Level.SEVERE))
				LOGGER.log(Level.SEVERE,
						"Error encountered while parsing the request");

			response.setContentType("text/html");

			JSONObject jsonObj = new JSONObject();
			jsonObj.put("success", false);
			jsonObj.put("errorMessage", ex.getLocalizedMessage());

			Utilities.writeResponse(response, jsonObj.toString(), LOGGER);

		}

		boolean error = false;
		String message = null;
		if (itemSize > this.callbackConfiguration.getMaxItems()) {
			error = true;
			message = "Max items size exceeded (expected: '"
					+ this.callbackConfiguration.getMaxItems() + "', found: '"
					+ itemSize + "').";
		} else if (maxSize > this.callbackConfiguration.getMaxSize()) {
			error = true;
			message = " Max item size exceeded (expected: '"
					+ this.callbackConfiguration.getMaxSize() + "', found: '"
					+ maxSize + "' on item '" + itemName + "').";
		}
		if (error) {
			callbackResult.setSuccess(false);
			Utilities.writeResponse(response,
					"{ \"success\":false, \"errorMessage\":\"" + message
							+ "\"}", null);
		} else {
			callbackResult.setSuccess(true);
		}

		return callbackResult;
	}

	/**
	 * @return the callbackConfiguration
	 */
	public FileUploadCallbackConfiguration getCallbackConfiguration() {
		return callbackConfiguration;
	}

	/**
	 * @param callbackConfiguration
	 *            the callbackConfiguration to set
	 */
	public void setCallbackConfiguration(
			FileUploadCallbackConfiguration callbackConfiguration) {
		this.callbackConfiguration = callbackConfiguration;
	}

}