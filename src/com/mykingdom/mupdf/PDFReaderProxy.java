/**
 * This file was auto-generated by the Titanium Module SDK helper for Android
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 */
package com.mykingdom.mupdf;

import java.io.File;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiFileProxy;
import org.appcelerator.titanium.util.Log;
import org.appcelerator.titanium.util.TiConfig;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutArrangement;
import org.appcelerator.titanium.view.TiUIView;

import com.artifex.mupdflib.FilePicker;
import com.artifex.mupdflib.MuPDFCore;
import com.artifex.mupdflib.MuPDFPageAdapter;
import com.artifex.mupdflib.MuPDFReaderView;
import com.artifex.mupdflib.PDFPreviewGridActivityData;
import com.artifex.mupdflib.PageView;
import com.artifex.mupdflib.ReaderView;
import com.artifex.mupdflib.SearchTaskResult;
import com.artifex.mupdflib.FilePicker.FilePickerSupport;
import com.artifex.mupdflib.SearchTask;

import android.app.Activity;
import android.net.Uri;
import android.os.Message;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

// This proxy can be created by calling Mupdf.createPDFReader({file: "path"})
@Kroll.proxy(creatableInModule = MupdfModule.class)
public class PDFReaderProxy extends TiViewProxy {

	// Standard Debugging variables
	private static final String LCAT = "PDFReaderProxy";
	private static final boolean DBG = TiConfig.LOGD;

	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;
	private static final int MSG_SET_CURRENT_PAGE = MSG_FIRST_ID + 100;

	private class PDFReaderView extends TiUIView implements FilePickerSupport {

		// Static Properties
		public static final String PROPERTY_FILE_PATH = "file";
		public static final String PROPERTY_CURRENT_PAGE = "currentPage";
		public static final String PROPERTY_PAGE_COUNT = "pageCount";
		public static final String EVENT_PAGE_CHANGED = "pagechanged";
		public static final String PROPERTY_X = "x";
		public static final String PROPERTY_Y = "y";
		public static final String EVENT_CLICK = "click";

		private final int TAP_PAGE_MARGIN = 5;
		private String mFileName;
		private MuPDFReaderView mDocView;
		private MuPDFCore core;
		private SearchTask mSearchTask;
		private KrollFunction mSearchCallback;
		private TiApplication tiApplication;
		private int currentPage;
		private int pageCount;

		public PDFReaderView(TiViewProxy proxy) {

			super(proxy);

			LayoutArrangement arrangement = LayoutArrangement.DEFAULT;

			if (proxy.hasProperty(TiC.PROPERTY_LAYOUT)) {
				String layoutProperty = TiConvert.toString(proxy
						.getProperty(TiC.PROPERTY_LAYOUT));
				if (layoutProperty.equals(TiC.LAYOUT_HORIZONTAL)) {
					arrangement = LayoutArrangement.HORIZONTAL;
				} else if (layoutProperty.equals(TiC.LAYOUT_VERTICAL)) {
					arrangement = LayoutArrangement.VERTICAL;
				}
			}

			tiApplication = TiApplication.getInstance();

			TiCompositeLayout view = new TiCompositeLayout(proxy.getActivity(),
					arrangement);
			mDocView = new MuPDFReaderView(getActivity()) {
				@Override
				protected void onMoveToChild(int i) {
					currentPage = i + 1;
					if (getProxy().hasListeners(EVENT_PAGE_CHANGED)) {
						KrollDict data = new KrollDict();
						data.put(PROPERTY_CURRENT_PAGE, currentPage);
						data.put(PROPERTY_PAGE_COUNT, pageCount);
						getProxy().fireEvent(EVENT_PAGE_CHANGED, data);
					}
					super.onMoveToChild(i);
				}

				@Override
				protected void onTapMainDocArea() {
					if (getProxy().hasListeners(EVENT_CLICK)) {
						getProxy().fireEvent(EVENT_CLICK, null);
					}
				}
			};
			view.addView(mDocView);
			setNativeView(view);
		}

		@Override
		public void processProperties(KrollDict d) {

			if (d.containsKey(PROPERTY_FILE_PATH)) {
				try {
					TiFileProxy fileProxy = (TiFileProxy) d
							.get(PROPERTY_FILE_PATH);
					File file = fileProxy.getBaseFile().getNativeFile();
					if (file.exists()) {
						Log.i(LCAT, "PDF exists, trying to load");
						core = openFile(Uri.decode(Uri.fromFile(file)
								.getEncodedPath()));
						mDocView.setAdapter(new MuPDFPageAdapter(getActivity(),
								this, core));
						pageCount = core.countPages();
						currentPage = 1;
					}
				} catch (Exception ex) {
					String err = (ex.getMessage() == null) ? "Something wrong with the file given"
							: ex.getMessage();
					Log.e(LCAT, err);
				}
			}

			super.processProperties(d);
		}
		
		public void cleanup()
		{
			mDocView.releaseViews();
			core.onDestroy();
			core = null;
			mSearchTask = null;
			mDocView = null;
			mSearchCallback = null;
		}

		private MuPDFCore openFile(String path) {
			int lastSlashPos = path.lastIndexOf('/');
			mFileName = new String(lastSlashPos == -1 ? path
					: path.substring(lastSlashPos + 1));
			System.out.println("Trying to open " + path);
			try {
				core = new MuPDFCore(getActivity(), path);
				mSearchTask = new SearchTask(tiApplication, core) {

					@Override
					protected void onTextFound(SearchTaskResult result) {
						SearchTaskResult.set(result);
						mDocView.setDisplayedViewIndex(result.pageNumber);
						mDocView.resetupChildren();
						if (mSearchCallback != null) {
							HashMap<String, Object> params = new HashMap<String, Object>();
							params.put("count", result.searchBoxes.length);
							params.put("pageNumber", result.pageNumber);
							params.put("error", false);
							params.put("success", true);
							mSearchCallback.call(getKrollObject(), params);
						}
					}

				};
				// New file: drop the old outline data
				// OutlineActivityData.set(null);
				PDFPreviewGridActivityData.set(null);
			} catch (Exception e) {
				System.out.println(e);
				return null;
			}
			return core;
		}

		public int getPageCount() {
			return pageCount;
		}

		public void setCurrentPage(int pageNumber) {
			// pageNumber - converting 1 based index to 0
			mDocView.setDisplayedViewIndex(pageNumber - 1);
		}

		public int getCurrentPage() {
			return currentPage;
		}

		public void moveToNext() {
			if (core == null)
				return;
			mDocView.moveToNext();
		}

		public void moveToPrevious() {
			if (core == null)
				return;
			mDocView.moveToPrevious();
		}

		public void onSearch(KrollFunction callback) {
			mSearchCallback = callback;
		}

		public void search(String key, int direction) {
			if (mSearchTask == null)
				return;
			int displayPage = mDocView.getDisplayedViewIndex();
			SearchTaskResult r = SearchTaskResult.get();
			int searchPage = r != null ? r.pageNumber : -1;
			mSearchTask.go(key, direction, displayPage, searchPage);
		}
		@Override
		public void performPickFor(FilePicker picker) {

		}

	}

	// Constructor
	public PDFReaderProxy() {
		super();
	}

	@Override
	public TiUIView createView(Activity activity) {
		TiUIView view = new PDFReaderView(this);
		view.getLayoutParams().autoFillsHeight = true;
		view.getLayoutParams().autoFillsWidth = true;
		return view;
	}

	public PDFReaderView getPDFReaderView() {
		return (PDFReaderView) getOrCreateView();
	}

	@Override
	public void releaseViews()
	{	
		// Release PDF Viewer Memory
		Log.d("MUPDF", "RELEASING VIEW");
		getPDFReaderView().cleanup();
		
		super.releaseViews();
	}
	
	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_SET_CURRENT_PAGE: {
			getPDFReaderView().setCurrentPage((Integer) msg.obj);
			return true;
		}
		default: {
			return super.handleMessage(msg);
		}
		}
	}

	// Handle creation options
	@Override
	public void handleCreationDict(KrollDict options) {
		super.handleCreationDict(options);
	}

	@Kroll.method
	public void loadPDFFromFile(TiFileProxy file) {
		setPropertyAndFire(PDFReaderView.PROPERTY_FILE_PATH, file);
	}

	@Kroll.method
	public int getPageCount() {
		return getPDFReaderView().getPageCount();
	}

	@Kroll.method
	public void setCurrentPage(int pageNumber) {
		if (TiApplication.isUIThread()) {
			getPDFReaderView().setCurrentPage(pageNumber);
			return;
		}
		Message message = getMainHandler().obtainMessage(MSG_SET_CURRENT_PAGE,
				pageNumber);
		message.sendToTarget();
	}

	@Kroll.method
	public int getCurrentPage() {
		return getPDFReaderView().getCurrentPage();
	}

	@Kroll.method
	public void moveToNext() {
		getPDFReaderView().moveToNext();
	}

	@Kroll.method
	public void moveToPrevious() {
		getPDFReaderView().moveToPrevious();
	}

	@Kroll.method
	public void onSearch(KrollFunction callback) {
		getPDFReaderView().onSearch(callback);
	}

	@Kroll.method
	public void search(String key, int direction) {
		getPDFReaderView().search(key, direction);
	}
}