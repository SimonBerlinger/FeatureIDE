/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2010  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.ui.views.collaboration;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.ConnectionLayer;
import org.eclipse.gef.EditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.listeners.ICurrentBuildListener;
import de.ovgu.featureide.fm.ui.editors.featuremodel.GUIDefaults;
import de.ovgu.featureide.ui.UIPlugin;
import de.ovgu.featureide.ui.views.collaboration.action.AddRoleAction;
import de.ovgu.featureide.ui.views.collaboration.action.DeleteAction;
import de.ovgu.featureide.ui.views.collaboration.action.FilterAction;
import de.ovgu.featureide.ui.views.collaboration.action.ShowUnselectedAction;
import de.ovgu.featureide.ui.views.collaboration.editparts.GraphicalEditPartFactory;
import de.ovgu.featureide.ui.views.collaboration.model.Collaboration;
import de.ovgu.featureide.ui.views.collaboration.model.CollaborationModel;
import de.ovgu.featureide.ui.views.collaboration.model.CollaborationModelBuilder;

/**
 * View of the collaboration diagram.
 * 
 * @author Constanze Adler
 * @author Jens Meinicke
 * @author Stephan Besecke
 */
public class CollaborationView extends ViewPart implements GUIDefaults, ICurrentBuildListener{
	
	public static final String ID = UIPlugin.PLUGIN_ID + ".views.collaboration.Collaboration";
	private GraphicalViewerImpl viewer;
	public CollaborationModelBuilder builder = new CollaborationModelBuilder();
	private CollaborationModel model = new CollaborationModel();
	
	private AddRoleAction addRoleAction;
	private DeleteAction delAction;
	private Action toolbarAction;
	private FilterAction filterAction;
	private ShowUnselectedAction showUnselectedAction; 
	
	private IFeatureProject featureProject;
	
	public IFeatureProject getFeatureProject() {
		return featureProject;
	}
	
	private IPartListener editorListener = new IPartListener() {

		public void partOpened(IWorkbenchPart part) {
		}

		public void partDeactivated(IWorkbenchPart part) {
		}

		public void partClosed(IWorkbenchPart part) {
		}

		public void partBroughtToTop(IWorkbenchPart part) {
			if (part instanceof IEditorPart)
				setEditorActions(part);
		}

		public void partActivated(IWorkbenchPart part) {
			if (part instanceof IViewPart)
				setEditorActions(part);
		}

	};
	
	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		IWorkbenchWindow editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IEditorPart part = null;
		if (editor != null) {
			IWorkbenchPage page = editor.getActivePage();
			if (page != null) {
				part = page.getActiveEditor();
			}
		}
		
		viewer = new ScrollingGraphicalViewer();
		viewer.createControl(parent);
		viewer.getControl().setBackground(DIAGRAM_BACKGROUND);
		getSite().getPage().addPartListener(editorListener); // EditorListener
		CorePlugin.getDefault().addCurrentBuildListener(this); // BuildListener
		
		ScalableFreeformRootEditPart rootEditPart = new ScalableFreeformRootEditPart(); // required for borders
		((ConnectionLayer) rootEditPart
				.getLayer(LayerConstants.CONNECTION_LAYER))
				.setAntialias(SWT.ON);

		viewer.setRootEditPart(rootEditPart);
		viewer.setEditDomain(new EditDomain());
		viewer.setEditPartFactory(new GraphicalEditPartFactory());
		
		createContextMenu();
		createActions(part);
		makeActions();
		contributeToActionBars();
	}

	 private void contributeToActionBars() {
		 IActionBars bars = getViewSite().getActionBars();
		 fillLocalToolBar(bars.getToolBarManager());
	 }
	
	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	private void setEditorActions(IWorkbenchPart activeEditor) {
		IEditorPart part = null;
		if (activeEditor != null) {
			IWorkbenchPage page = activeEditor.getSite().getPage();
			if (page != null) {
				part = page.getActiveEditor();
				if (part != null) {
					//case: open editor
					FileEditorInput inputFile = (FileEditorInput)part.getEditorInput();
					featureProject = CorePlugin.getFeatureProject(inputFile.getFile());
					if (featureProject != null) {
						//case: its a featureIDE project
						if (inputFile.getName().endsWith(".equation") || inputFile.getName().endsWith(".config")) {
							//case: open configuration editor
							if (builder.configuration.equals(inputFile.getName())) {
								return;
							} else {
								builder.configuration = inputFile.getName();
							}
							
						} else if (featureProject != null){
							//case: open editor is no configuration editor
							IFile file = featureProject.getCurrentConfiguration();
							if (file != null) {
								if (builder.configuration.equals(file.getName())) {
									return;
								} else {
									builder.configuration = file.getName();
								}
							} else {
								builder.configuration = "";
							}
						} else {
							return;
						}
					}
				}
			}
		}
		
		if (featureProject == null) {
			model = new CollaborationModel();
			model.collaborations.add(new Collaboration("Open a file from a FeatureIDE project"));
			viewer.setContents(model);
		} else {
			if (featureProject.getCurrentConfiguration() == null){
				model = new CollaborationModel();
				model.collaborations.add(new Collaboration("Please create a new configuration file"));
				viewer.setContents(model);
			} else
				updateGuiAfterBuild(featureProject);
		}
	}
	
	private void createContextMenu() {
		
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		
		menuMgr.addMenuListener(new IMenuListener(){
			public void menuAboutToShow(IMenuManager m){
				fillContextMenu(m);
			}
		});
		
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}
	
	private void fillContextMenu(IMenuManager menuMgr){
		boolean isEmpty = viewer.getSelection().isEmpty();
		addRoleAction.setEnabled(!isEmpty);
		filterAction.setEnabled(!isEmpty);
		delAction.setEnabled(!isEmpty);
		showUnselectedAction.setEnabled(!isEmpty);
		
		if (featureProject.getComposer().getName().equals("AHEAD"))
			menuMgr.add(addRoleAction);
		menuMgr.add(filterAction);
		menuMgr.add(showUnselectedAction);
		menuMgr.add(delAction);
	}
	
	private void createActions(IEditorPart part) {
		addRoleAction	= new AddRoleAction("Add new Class / Role", viewer, this);
		delAction		= new DeleteAction("Delete", viewer);
		filterAction	= new FilterAction("Filter",viewer,this,model);
		showUnselectedAction = new ShowUnselectedAction("Show unselected features",viewer,this,model);
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(toolbarAction);
		toolbarAction.setToolTipText("Build collaborationmodel");
		toolbarAction.setImageDescriptor(ImageDescriptor.createFromImage(UIPlugin.getImage("refresh_tab.gif")));
	}
	
	private void makeActions() {
		toolbarAction = new Action() {
			public void run() {
				Job job = new Job("toolbarAction") {
					protected IStatus run(IProgressMonitor monitor) {
						if (!toolbarAction.isEnabled())
							return Status.OK_STATUS;
						toolbarAction.setEnabled(false);
						updateGuiAfterBuild(featureProject);
						return Status.OK_STATUS;
					}
				};
				job.setPriority(Job.SHORT);
				job.schedule();
			}
		};
	}
	
	/* (non-Javadoc)
	 * @see de.ovgu.featureide.core.listeners.ICurrentBuildListener#updateGuiAfterBuild(de.ovgu.featureide.core.IFeatureProject)
	 */
	public void updateGuiAfterBuild(final IFeatureProject project) {		
		Job job = new Job("buildCollaborationModel") {
			public IStatus run(IProgressMonitor monitor) {
				model = builder.buildCollaborationModel(project);
				if (model == null) {
					toolbarAction.setEnabled(true);
					return Status.OK_STATUS;
				}
				
				UIJob uiJob = new UIJob("updateCollaborationView") {
					public IStatus runInUIThread(IProgressMonitor monitor) {
						viewer.setContents(model);		
						EditPart part = viewer.getContents();
						if (part != null) {
							part.refresh();
						}
						toolbarAction.setEnabled(true);
						return Status.OK_STATUS;
					}
				};
				uiJob.setPriority(Job.DECORATE);
				uiJob.schedule();
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.DECORATE);
		job.schedule();
	}
}