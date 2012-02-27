package org.openimaj.demos.sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.content.animation.AnimatedVideo;
import org.openimaj.content.animation.animator.DoubleArrayValueAnimator;
import org.openimaj.content.animation.animator.ForwardBackwardLoopingValueAnimator;
import org.openimaj.content.animation.animator.LinearDoubleValueAnimator;
import org.openimaj.content.animation.animator.LoopingValueAnimator;
import org.openimaj.content.animation.animator.ValueAnimator;
import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.FImage;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.PointDistributionModel;
import org.openimaj.math.geometry.shape.PointList;
import org.openimaj.math.geometry.transforms.TransformUtilities;
import org.openimaj.video.VideoDisplay;

public class PDMPlayground3 {
	static PointList readPts(File file) throws IOException {
		PointList pl = new PointList();
		BufferedReader br = new BufferedReader(new FileReader(file));

		br.readLine();
		br.readLine();
		br.readLine();

		String line;
		while ((line = br.readLine()) != null) {
			if (!line.startsWith("}") && line.trim().length() > 0) {
				String[] parts = line.split("\\s+");

				float x = Float.parseFloat(parts[0].trim());
				float y = Float.parseFloat(parts[1].trim());

				pl.points.add(new Point2dImpl(x, y));
			}
		}
		br.close();

		return pl;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		File dir = new File("/Users/jsh2/Downloads/am_tools/points/");

		File[] fileList = new File[] {
				new File(dir, "107_0764.pts"),
				new File(dir, "107_0766.pts"),
				new File(dir, "107_0779.pts"),
				new File(dir, "107_0780.pts"),
				new File(dir, "107_0781.pts"),
				new File(dir, "107_0782.pts"),
				new File(dir, "107_0784.pts"),
				new File(dir, "107_0785.pts"),
				new File(dir, "107_0786.pts"),
				new File(dir, "107_0787.pts"),
				new File(dir, "107_0788.pts"),
				new File(dir, "107_0789.pts"),
				new File(dir, "107_0790.pts"),
				new File(dir, "107_0791.pts"),
				new File(dir, "107_0792.pts")
		};

		List<PointList> pls = new ArrayList<PointList>();
		for (File f : fileList) {
			pls.add(readPts(f));
		}

		final PointDistributionModel pdm = new PointDistributionModel(pls);

		FImage img = new FImage(200,200);
		img.drawPoints(pdm.getMean().transform(TransformUtilities.translateMatrix(100, 100).times(TransformUtilities.scaleMatrix(50, 50))), 1f, 1);
		DisplayUtilities.display(img);

		pdm.setNumComponents(20);

		final double sd = pdm.getStandardDeviations(3.0)[0];
		
		VideoDisplay.createVideoDisplay(new AnimatedVideo<FImage>(new FImage(200,200)) {
			ValueAnimator<Double> a = ForwardBackwardLoopingValueAnimator.loop(new LinearDoubleValueAnimator(-sd, sd, 60)); 

			@Override
			protected void updateNextFrame(FImage frame) {
				frame.fill(0f);

				//PointList newShape = pdm.generateNewShape( new double[] {a.nextValue()} );
				PointList newShape = pdm.generateNewShape( new double[] {-sd} );
				frame.drawPoints(newShape.transform(TransformUtilities.translateMatrix(100, 100).times(TransformUtilities.scaleMatrix(50, 50))), 1f, 1);
			}
		});		
	}
}
