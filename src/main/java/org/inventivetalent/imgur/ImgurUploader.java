package org.inventivetalent.imgur;

import com.google.common.io.BaseEncoding;
import com.google.gson.JsonParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple class to upload images to imgur.com
 */
public class ImgurUploader {

	public static Executor uploadExecutor = Executors.newCachedThreadPool();

	/**
	 * Upload an image asynchronously
	 *
	 * @param clientId       generated client ID ( <a href="https://api.imgur.com/oauth2/addclient">https://api.imgur.com/oauth2/addclient</a> )
	 * @param image          image to upload
	 * @param uploadCallback callback to inform about the upload status
	 */
	public static void upload(final String clientId, final RenderedImage image, final UploadCallback uploadCallback) {
		checkNotNull(clientId);
		checkNotNull(image);
		checkNotNull(uploadCallback);
		uploadExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					Connection.Response response = uploadSync(clientId, image);
					uploadCallback.uploaded(response.headers(), new JsonParser().parse(response.body()));
				} catch (Throwable throwable) {
					uploadCallback.exception(throwable);
				}
			}
		});
	}

	/**
	 * Upload an image
	 *
	 * @param clientId generated client ID
	 * @param image    image to upload
	 * @return the received {@link org.jsoup.Connection.Response}
	 * @throws IOException if the upload or image conversion fails
	 * @see #upload(String, RenderedImage, UploadCallback)
	 */
	public static Connection.Response uploadSync(final String clientId, final RenderedImage image) throws IOException {
		Connection connection = Jsoup.connect("https://api.imgur.com/3/image");
		connection.timeout(10000).userAgent("InventiveImgurUploader").ignoreContentType(true).ignoreHttpErrors(true).method(Connection.Method.POST);
		connection.header("Authorization", "Client-ID " + clientId);
		connection.header("Content-Type", "application/x-www-form-urlencoded");

		ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
		ImageIO.write(image, "png", byteArray);
		byte[] byteImage = byteArray.toByteArray();
		String dataImage = BaseEncoding.base64().encode(byteImage);
		connection.data("image", dataImage);

		return connection.execute();
	}

}
