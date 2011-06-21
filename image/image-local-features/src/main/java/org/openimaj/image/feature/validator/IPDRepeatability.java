package org.openimaj.image.feature.validator;

import gnu.trove.TDoubleArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.Image;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.colour.Transforms;
import org.openimaj.image.feature.local.interest.AbstractIPD.InterestPointData;
import org.openimaj.image.feature.local.interest.InterestPointDetector;
import org.openimaj.image.feature.validator.IPDRepeatability.ScoredPair;
import org.openimaj.math.geometry.line.Line2d;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.shape.Ellipse;
import org.openimaj.math.geometry.shape.EllipseUtilities;
import org.openimaj.math.geometry.shape.Shape;
import org.openimaj.math.geometry.transforms.TransformUtilities;
import org.openimaj.math.matrix.MatrixUtils;
import org.openimaj.util.pair.Pair;

import Jama.Matrix;

/**
 * An interest point repeatability as originally implemented here:
 * 
 * http://www.robots.ox.ac.uk/~vgg/research/affine/evaluation.html
 * 
 * We find some interest points in two images, and the known homography to go from image 1 to image 2
 * 
 * We apply this exhaustively to a pairwise matching of each feature to each other feature and compare the distances
 * of the transformed features from the second image to the features in the first image. If the pair distance is below
 * a give threshold they are placed on top of each other and their overlap measured.
 * 
 * Repeatability is measured at a given overlap threshold, if two feature point ellipses overlap over a certain percentage of 
 * their overall size then those features are counted as repeatable. The repeatability of a given IPD for a 
 * given pair of images is the proportion of repeatable features for a given maximum distance and a given 
 * overlap percentage.
 * @author Jonathon Hare <jsh2@ecs.soton.ac.uk>, Sina Samangooei <ss@ecs.soton.ac.uk>
 *
 */
public class IPDRepeatability {
	private MBFImage image1;
	private MBFImage image2;
	private List<InterestPointData> image1Points;
	private List<InterestPointData> image2Points;
	private Matrix homography;
	private List<InterestPointData> validImage2Points;

	/**
	 * Check the repeatability against two imags, two sets of points and a homography between the two images.
	 * 
	 * @param image1
	 * @param image2
	 * @param image1Points
	 * @param image2Points
	 * @param homography
	 */
	public IPDRepeatability(MBFImage image1, MBFImage image2, List<InterestPointData> image1Points,List<InterestPointData> image2Points,Matrix homography){
		setup(image1,image2,image1Points,image2Points,homography);
	}
	
	/**
	 * Check the repeatability between two images from files, an interest point detector used to find the feature points
	 * in the images and a homography from a file. The homography file has the format:
	 * 
	 * number	number	number
	 * number	number	number
	 * number	number	number
	 * 
	 * @param image1f
	 * @param image2f
	 * @param ipd
	 * @param homographyf
	 * @throws IOException
	 */
	public IPDRepeatability(File image1f, File image2f, InterestPointDetector ipd,File homographyf) throws IOException{
		MBFImage image1 = ImageUtilities.readMBF(image1f);
		MBFImage image2 = ImageUtilities.readMBF(image2f);
		
		ipd.findInterestPoints(Transforms.calculateIntensityNTSC(image1));
		List<InterestPointData> image1Points = ipd.getInterestPoints(20);
		ipd.findInterestPoints(Transforms.calculateIntensityNTSC(image2));
		List<InterestPointData> image2Points = ipd.getInterestPoints(20);
		
		Matrix homography = readHomography(homographyf);
		setup(image1,image2,image1Points,image2Points,homography);
	}
	
	/**
	 * Two images, features extracted using ipd, homography found in stream. See {@link IPDRepeatability}
	 * 
	 * @param image1
	 * @param image2
	 * @param ipd
	 * @param homographyf
	 * @throws IOException
	 */
	public IPDRepeatability(MBFImage image1, MBFImage image2, InterestPointDetector ipd,InputStream homographyf) throws IOException{
		
		ipd.findInterestPoints(Transforms.calculateIntensityNTSC(image1));
		List<InterestPointData> image1Points = ipd.getInterestPoints(20);
		ipd.findInterestPoints(Transforms.calculateIntensityNTSC(image2));
		List<InterestPointData> image2Points = ipd.getInterestPoints(20);
		
		Matrix homography = readHomography(homographyf);
		setup(image1,image2,image1Points,image2Points,homography);
	}
	
	/**
	 * Two images, features extracted using ipd, homography matrix between the two images
	 * 
	 * @param image1
	 * @param image2
	 * @param ipd
	 * @param homography
	 * @throws IOException
	 */
	public IPDRepeatability(MBFImage image1, MBFImage image2, InterestPointDetector ipd,Matrix homography) throws IOException{
		
		ipd.findInterestPoints(Transforms.calculateIntensityNTSC(image1));
		List<InterestPointData> image1Points = ipd.getInterestPoints(20);
		ipd.findInterestPoints(Transforms.calculateIntensityNTSC(image2));
		List<InterestPointData> image2Points = ipd.getInterestPoints(20);
		
		setup(image1,image2,image1Points,image2Points,homography);
	}

	private void setup(MBFImage image1, MBFImage image2,List<InterestPointData> image1Points,List<InterestPointData> image2Points, Matrix homography) {
		this.image1 = image1; this.image2 = image2; this.image1Points = image1Points; this.image2Points = image2Points; this.homography = homography;
		
		Shape validArea = this.image1.getBounds().transform(homography);
		this.validImage2Points = new ArrayList<InterestPointData>();
		for(InterestPointData data : image2Points){
			if(validArea.isInside(data)){
				this.validImage2Points.add(data);
			}else{
			}
		}
	}
	
	
	/**
	 * Find pairs of interest points whose ellipses overlap sufficiently and calculate how much they overlap. 
	 * 
	 * @return map of an interest point pair to a percentage overlap (0 > overlap =<1.0
	 */
	public PriorityQueue<ScoredPair<Pair<Integer>>> calculateOverlappingEllipses(){
		return calculateOverlappingEllipses(4); // defualt in the oxford code
	}
	/**
	 * Find pairs of interest points whose ellipses overlap sufficiently and calculate how much they overlap. This function must be told
	 * what maximum distance factor is which two interest points are considered to match and therefore their overlap measured.
	 * 
	 * @param maximumDistanceFactor 
	 * 
	 * @return map of an interest point pair to a percentage overlap (0 > overlap =<1.0
	 */
	public PriorityQueue<ScoredPair<Pair<Integer>>> calculateOverlappingEllipses(double maximumDistanceFactor){
		int smallerSetSize = Math.min(this.image1Points.size(), this.validImage2Points.size());
		PriorityQueue<ScoredPair<Pair<Integer>>>  overlapping = new PriorityQueue<ScoredPair<Pair<Integer>>>();
		Map<Integer,Matrix> matrixHash1 = new HashMap<Integer,Matrix>();
		Map<Integer,Matrix> matrixHash2 = new HashMap<Integer,Matrix>();
		int i =0; 
		for(InterestPointData firstImagePoint : this.image1Points)
		{
			int j = 0;
			Ellipse ellipse1 = EllipseUtilities.ellipseFromSecondMoments(firstImagePoint.x,firstImagePoint.y,firstImagePoint.secondMoments,firstImagePoint.scale);
			if(!matrixHash1.containsKey(i)) matrixHash1.put(i, EllipseUtilities.ellipseToCovariance(ellipse1));
			for(InterestPointData secondImagePoint : this.validImage2Points)
			{
				
				Ellipse ellipse2 = EllipseUtilities.ellipseFromSecondMoments(secondImagePoint.x,secondImagePoint.y,secondImagePoint.secondMoments,secondImagePoint.scale);
				ellipse2 = (Ellipse) ellipse2.transform(this.homography.inverse());
				if(!matrixHash2.containsKey(j)) matrixHash2.put(j, EllipseUtilities.ellipseToCovariance(ellipse2));
				double overlap = calculateOverlapPercentage(matrixHash1.get(i),matrixHash2.get(j),ellipse1, ellipse2,maximumDistanceFactor);
				if(overlap > 0){
					overlapping.add(new ScoredPair<Pair<Integer>>(new Pair<Integer>(i,j), overlap));
				}
				j++;
			}
			i++;
		}
		PriorityQueue<ScoredPair<Pair<Integer>>> prunedOverlapping = pruneOverlapping(overlapping, smallerSetSize);
		return prunedOverlapping;
	}
	
	/**
	 * The percentage of valid points found to be repeatable. Repeatability of a given ellipse is defined by what percentage overlap there
	 * is between it and a nearby detected ellipse (after an affine transform). The repeatability of all the points in an image is defined by the proprotion
	 * of points which could catch and did match with their ellipses overlapping by more than or equal to the percentageOverlap (1 == complete overlap, 0 == no overlap)
	 * 
	 * @param percentageOverlap the percentage overlap two ellipses must be over to be considered a "repeatable" point
	 * @param maximumDistanceMultiple The distance multiple at which point two interest points are considered to be "close"
	 * @return the percentage of ellipses which are repeatable 
	 */
	public static double repeatability(Image<?,?> img1, Image<?,?> img2, List<Ellipse> e1, List<Ellipse> e2, Matrix transform, double percentageOverlap, double maximumDistanceMultiple){
		List<Ellipse> validE2 = validPoints(e2,img1,transform);
		List<Ellipse> validE1 = validPoints(e1,img2,transform.inverse());
		PriorityQueue<ScoredPair<Pair<Integer>>> map = calculateOverlappingEllipses(validE1,validE2,transform, maximumDistanceMultiple);
		
		double potentialMatches = Math.min(validE1.size(), validE2.size());
		double countMatches = 0;
		for(ScoredPair<?> scoredPair: map){
			if(scoredPair.score > percentageOverlap)
				countMatches ++ ;
		}
		
		return countMatches / potentialMatches;
	}
	
	public static List<Ellipse> validPoints(List<Ellipse> e2,Image<?, ?> img1, Matrix transform) {
		List<Ellipse> validE2 = new ArrayList<Ellipse>();
		Shape validArea = img1.getBounds().transform(transform);
		for(Ellipse data : e2){
			if(validArea.isInside(data.getCOG())){
				validE2.add(data);
			}else{
			}
		}
		return validE2;
	}
	
	static class ScoredPair<T extends Pair<?>> implements Comparable<ScoredPair<T>>{
		private T pair;
		private double score;

		public ScoredPair(T pair, double score){
			this.pair = pair;
			this.score = score;
		}

		@Override
		public int compareTo(ScoredPair<T> o) { return -Double.compare(this.score, o.score); }
	}
	
	public static PriorityQueue<ScoredPair<Pair<Integer>>> calculateOverlappingEllipses(List<Ellipse> firstImagePoints, List<Ellipse> secondImagePoints,Matrix transform, double maximumDistanceMultiple) {
		int smallerSetSize = Math.min(firstImagePoints.size(), secondImagePoints.size());
		PriorityQueue<ScoredPair<Pair<Integer>>> overlapping = new PriorityQueue<ScoredPair<Pair<Integer>>>(smallerSetSize);
		// For each ellipse pair, add the pair to the queue if it's score is != 0 (i.e. there is any overlap)
		int i = 0; 
		Map<Integer,Matrix> matrixHash1 = new HashMap<Integer,Matrix>();
		Map<Integer,Matrix> matrixHash2 = new HashMap<Integer,Matrix>();
		for(Ellipse firstImagePoint : firstImagePoints)
		{	
			int j = 0;
			if(!matrixHash1.containsKey(i)) matrixHash1.put(i, EllipseUtilities.ellipseToCovariance(firstImagePoint));
			for(Ellipse secondImagePoint : secondImagePoints)
			{
				Ellipse correctedSecondImagePoint = (Ellipse) secondImagePoint.transform(transform.inverse());
				if(!matrixHash2.containsKey(j)) matrixHash2.put(j, EllipseUtilities.ellipseToCovariance(correctedSecondImagePoint));
				double overlap = calculateOverlapPercentage(matrixHash1.get(i),matrixHash2.get(j),firstImagePoint, correctedSecondImagePoint,maximumDistanceMultiple);
				
				if(overlap > 0){
					Pair<Integer> pairToAdd = new Pair<Integer>(i,j);
					overlapping.add(new ScoredPair<Pair<Integer>>(pairToAdd, overlap));
				}
				j++;
			}
			i++;
		}
		PriorityQueue<ScoredPair<Pair<Integer>>> prunedOverlapping = pruneOverlapping(overlapping, smallerSetSize);
		return prunedOverlapping;
	}
	
	

	private static PriorityQueue<ScoredPair<Pair<Integer>>> pruneOverlapping(PriorityQueue<ScoredPair<Pair<Integer>>> overlapping, int smallerSetSize) {
		// Use the priority queue to perform a greedy optimisation. 
		// Once you see a pair don't allow any other pair involving either element
		Set<Integer> seenE1 = new HashSet<Integer>();
		Set<Integer> seenE2 = new HashSet<Integer>();
		PriorityQueue<ScoredPair<Pair<Integer>>> prunedOverlapping = new PriorityQueue<ScoredPair<Pair<Integer>>>();
		while(overlapping.size() > 0 && seenE1.size() < smallerSetSize){
			ScoredPair<Pair<Integer>> scoredPair = overlapping.poll();
			if(!seenE1.contains(scoredPair.pair.firstObject()) && !seenE2.contains(scoredPair.pair.secondObject())){
				prunedOverlapping.add(scoredPair);
				seenE1.add(scoredPair.pair.firstObject());
				seenE2.add(scoredPair.pair.secondObject());
			}
		}
		return prunedOverlapping;
	}

	/**
	 * The percentage of valid points found to be repeatable. Repeatability of a given ellipse is defined by what percentage overlap there
	 * is between it and a nearby detected ellipse (after an affine transform). The repeatability of all the points in an image is defined by the proprotion
	 * of points which could catch and did match with their ellipses overlapping by more than or equal to the percentageOverlap (1 == complete overlap, 0 == no overlap)
	 * 
	 * @param percentageOverlap the percentage overlap two ellipses must be over to be considered a "repeatable" point
	 * @param maximumDistanceMultiple The distance multiple at which point two interest points are considered to be "close"
	 * @return the percentage of ellipses which are repeatable 
	 */
	public double repeatability(double percentageOverlap, double maximumDistanceMultiple){
		PriorityQueue<ScoredPair<Pair<Integer>>> map = calculateOverlappingEllipses(maximumDistanceMultiple);
		
		double potentialMatches = Math.min(this.validImage2Points.size(), this.image1Points.size());
		double countMatches = 0;
		for(ScoredPair<Pair<Integer>> d : map){
			if(d.score > percentageOverlap)
				countMatches ++ ;
		}
		
		return countMatches / potentialMatches;
	}
	
	public static Matrix readHomography(File homographyf) throws IOException {
		return readHomography(new FileInputStream(homographyf));
	}
	public static Matrix readHomography(InputStream homographyf) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(homographyf));
		List<TDoubleArrayList> doubleListList = new ArrayList<TDoubleArrayList>();
		String line = null;
		int nCols = -1;
		int nRows = 0;
		while((line = reader.readLine())!=null){
			boolean anyAdded = false;
			String[] parts = line.split(" ");
			TDoubleArrayList doubleList = new TDoubleArrayList();
			int currCols= 0;
			for(String part : parts){
				if(part.length() != 0){
					anyAdded = true;
					doubleList.add(Double.parseDouble(part));
					currCols++;
				}
			}
			if(nCols == -1) nCols = currCols;
			if(currCols!=nCols) throw new IOException("Could not read matrix file");
			if(anyAdded){
				doubleListList.add(doubleList);
				nRows ++;
			}
		}
		Matrix ret = new Matrix(nRows,nCols);
		int rowNumber = 0;
		for(TDoubleArrayList doubleList: doubleListList){
			doubleList.toNativeArray(ret.getArray()[rowNumber++], 0, nCols);
		}
		return ret;
	}
	
	/**
	 * The overlap of a pair of ellipses
	 * @param e1
	 * @param e2
	 * @return the percentage overlap with a maximu distance of 4 * scale by ellipse largest ellipse
	 */
	public static double calculateOverlapPercentage(Ellipse e1, Ellipse e2){
		return calculateOverlapPercentage(EllipseUtilities.ellipseToCovariance(e1),EllipseUtilities.ellipseToCovariance(e2),e1,e2,4);
	}
	
	/**
	 * The overlap of a pair of ellipses
	 * @param e1
	 * @param e2
	 * @param maximumDistanceFactor
	 * @return the percentage overlap with a maximum distance scaled by  maximumDistanceFactor * ellipse scale 
	 */
	public static double calculateOverlapPercentageNew(Ellipse e1, Ellipse e2, double maximumDistanceFactor){
		double maxDistance = 1/Math.sqrt((e1.getMajor() * e1.getMinor()));
		// This is a scaling of the two ellipses such that they fit in a normalised space
		double scaleFactor = 30 / maxDistance;
		scaleFactor = 1 / (scaleFactor * scaleFactor);
		maxDistance*=maximumDistanceFactor*2;
		
//		System.out.println("Maximum distance was: " + maxDistance);
		if(new Line2d(e1.getCOG(),e2.getCOG()).calculateLength() >= maxDistance) return 0;
		Matrix scaleMatrix = TransformUtilities.scaleMatrix(scaleFactor, scaleFactor);
		
		Matrix e1Trans = TransformUtilities.translateToPointMatrix(e1.getCOG(),new Point2dImpl(0,0));
		Matrix e2Trans = TransformUtilities.translateToPointMatrix(e2.getCOG(),new Point2dImpl(0,0));
		Ellipse e1Corrected = (Ellipse) e1.transform(scaleMatrix.times(e1Trans));
		Ellipse e2Corrected = (Ellipse) e2.transform(scaleMatrix.times(e2Trans));
		
		double e1Area = e1Corrected.calculateArea();
		double e2Area = e2Corrected.calculateArea();
		
		double intersection = e1Corrected.intersectionArea(e2Corrected, 100);
		return intersection/( (e1Area - intersection) + (e2Area - intersection) + intersection);
	}
	
	/**
	 * This is how the original matlab found the difference between two ellipses. Basically, if ellipse 1 and 2 were
	 * within a certain distance the ellipses were placed on top of each other (i.e. same centroid) and the difference between
	 * them calculated. This is simulated in a much saner way in calculateOverlapPercentage
	 * @param e1Mat
	 * @param e2Mat
	 * @param e1
	 * @param e2
	 * @return the overlap percentage as calculated the matlab way (uses the covariance matricies of the ellipses)
	 */
	public static double calculateOverlapPercentage(Matrix e1Mat,Matrix e2Mat,Ellipse e1, Ellipse e2, double multiplier){
		e1Mat = EllipseUtilities.ellipseToCovariance(e1); 
		e2Mat = EllipseUtilities.ellipseToCovariance(e2);
		double maxDistance = 1/Math.sqrt((e1.getMajor() * e1.getMinor()));
		
		// This is a scaling of the two ellipses such that they fit in a normalised space
		double scaleFactor = 30 / maxDistance;
		scaleFactor = 1 / (scaleFactor * scaleFactor);
		maxDistance*=multiplier;
		if(new Line2d(e1.getCOG(),e2.getCOG()).calculateLength() >= maxDistance) return 0;
//		System.out.println(maxDistance);
		float dx = e2.getCOG().getX() - e1.getCOG().getX();
		float dy = e2.getCOG().getY() - e1.getCOG().getY();
		
		double yy1 = e1Mat.get(1, 1) * scaleFactor;
		double xx1 = e1Mat.get(0, 0) * scaleFactor;
		double xy1 = e1Mat.get(0, 1) * scaleFactor;
		
		double yy2 = e2Mat.get(1, 1) * scaleFactor;
		double xx2 = e2Mat.get(0, 0) * scaleFactor;
		double xy2 = e2Mat.get(0, 1) * scaleFactor;
		
		double e1Width = Math.sqrt(yy1/(xx1*yy1 - xy1*xy1));
		double e1Height = Math.sqrt(xx1/(xx1*yy1 - xy1*xy1));

		double e2Width = Math.sqrt(yy2/(xx2*yy2 - xy2*xy2));
		double e2Height = Math.sqrt(xx2/(xx2*yy2 - xy2*xy2));

		float maxx=(float) Math.ceil((e1Width>(dx+e2Width))?e1Width:(dx+e2Width));
		float minx=(float) Math.floor((-e1Width<(dx-e2Width))?(-e1Width):(dx-e2Width));
		float maxy=(float) Math.ceil((e1Height>(dy+e2Height))?e1Height:(dy+e2Height));
		float miny=(float) Math.floor((-e1Height<(dy-e2Height))?(-e1Height):(dy-e2Height));

		float mina=(maxx-minx)<(maxy-miny)?(maxx-minx):(maxy-miny);
		float dr=(float) (mina/50.0);
		int bua=0;int bna=0;int t1=0,t2=0;
		//compute the area
		for(float rx=minx;rx<=maxx;rx+=dr){
			float rx2=rx-dx;t1++;
			for(float ry=miny;ry<=maxy;ry+=dr){
				float ry2=ry-dy;
				//compute the distance from the ellipse center
				float a=(float) (xx1*rx*rx+2*xy1*rx*ry+yy1*ry*ry);
				float b=(float) (xx2*rx2*rx2+2*xy2*rx2*ry2+yy2*ry2*ry2);
				//compute the area
				if(a<1 && b<1)bna++;
				if(a<1 || b<1)bua++;
			}
		}
		return (double)bna/(double)bua;
	}
	
	/**
	 * Read an ellipses from the matlab interest point files
	 * @param file
	 * @return list of ellipses
	 * @throws IOException
	 */
	public static List<Ellipse> readMatlabInterestPoints(File file) throws IOException{
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		reader.readLine(); // 1.0
		reader.readLine(); // nPoints
		
		String line = "";
		List<Ellipse> ret = new ArrayList<Ellipse>();
		while((line = reader.readLine()) !=null){
			String[] parts = line.split(" ");
			
			float x = Float.parseFloat(parts[0]);
			float y = Float.parseFloat(parts[1]);
			float xx = Float.parseFloat(parts[2]);
			float xy = Float.parseFloat(parts[3]);
			float yy = Float.parseFloat(parts[4]);
			
			Ellipse e = EllipseUtilities.ellipseFromCovariance(x, y, new Matrix(new double[][]{{xx,xy},{xy,yy}}), 20.0f);
			ret.add(e);
		}
		return ret;
	}
	
	public static void main(String args[]) throws IOException{
		testSingleEllipseFromMatlab();
	}
	
	/**
	 * Check the overlap of a single ellipse using covariance numbrers loaded from matlab
	 * @throws IOException 
	 */
	public static void testSingleEllipseFromMatlab() throws IOException{
		MBFImage image = new MBFImage(800,800,ColourSpace.RGB);

//		Matrix covar1 = new Matrix(new double[][]{{0.002523,-0.000888},{-0.000888,0.000802}});
//		Ellipse e1 = EllipseUtilities.ellipseFromCovariance(185.130000f,139.150000f,covar1,1.0f);
//		Matrix covar2 = new Matrix(new double[][]{{0.000788,-0.000406},{-0.000406,0.000668}});
//		Ellipse e2 = EllipseUtilities.ellipseFromCovariance(185.287320f,137.549020f,covar2,1.0f);
		Matrix HI = IPDRepeatability.readHomography(IPDRepeatability.class.getResourceAsStream("/org/openimaj/image/feature/validator/graf/H1to2p"));
		Matrix H = HI.inverse();
		Matrix covar1 = new Matrix(new double[][]{{0.164293,-0.057837},{-0.057837,0.052221}});
		Ellipse e1 = EllipseUtilities.ellipseFromCovariance(185.130000f,139.150000f,covar1,1.0f);
		Matrix covar2 = new Matrix(new double[][]{{0.0511181,-0.0364547},{-0.0364547,0.0710733}});
		Ellipse e2 = EllipseUtilities.ellipseFromCovariance(161.051f,239.966f,covar2,1.0f);
		
		Ellipse e2Corrected = (Ellipse) e2.transform(H);
		Matrix e2CorrectedCovar = EllipseUtilities.ellipseToCovariance(e2Corrected);
		System.out.println(1-IPDRepeatability.calculateOverlapPercentage(covar1,e2CorrectedCovar,e1,e2Corrected ,4));
		System.out.println(IPDRepeatability.calculateOverlapPercentageNew(e1,e2, 4));
		
	}
}
