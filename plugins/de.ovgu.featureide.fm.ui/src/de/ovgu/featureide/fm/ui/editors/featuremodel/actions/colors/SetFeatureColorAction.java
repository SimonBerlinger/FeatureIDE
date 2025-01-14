/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2019  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 *
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.ui.editors.featuremodel.actions.colors;

import static de.ovgu.featureide.fm.core.localization.StringTable.COLORATION;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.color.ColorScheme;
import de.ovgu.featureide.fm.core.color.FeatureColor;
import de.ovgu.featureide.fm.core.color.FeatureColorManager;
import de.ovgu.featureide.fm.core.functional.Functional;
import de.ovgu.featureide.fm.core.io.EclipseFileSystem;
import de.ovgu.featureide.fm.core.io.manager.IFeatureModelManager;
import de.ovgu.featureide.fm.core.localization.StringTable;
import de.ovgu.featureide.fm.ui.FMUIPlugin;
import de.ovgu.featureide.fm.ui.editors.IGraphicalFeature;
import de.ovgu.featureide.fm.ui.editors.featuremodel.editparts.FeatureEditPart;
import de.ovgu.featureide.fm.ui.wizards.ColorSchemeWizard;

/**
 * ColorSelectedFeatureAction is the action that opens the ColorSelectedFeatureDialog with the selected features in the feature diagram.
 *
 * @author Christian Elzholz
 * @author Marcus Schmelz
 * @author Marcus Pinnecke
 * @author Paul Maximilian Bittner
 * @author Niklas Lehnfeld
 * @author Mohammed Mahhouk
 * @author Antje Moench
 */
public class SetFeatureColorAction extends Action {

	public static final String ID = "de.ovgu.featureide.setfeaturecolor";

	private static ImageDescriptor colorImage = FMUIPlugin.getDefault().getImageDescriptor("icons/FeatureColorIcon.gif");
	protected List<IFeature> featureList = new ArrayList<>();

	private boolean undoRedoEnabled = false;

	private IFeatureModelManager featureModelManager;

	public SetFeatureColorAction(ISelectionProvider viewer, IFeatureModelManager featureModelManager) {
		this(featureModelManager);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event.getSelection();
				final boolean validSelection = isSelectionValid(selection);
				setEnabled(validSelection);
				if (validSelection) {
					updateFeatureList(selection);
				}
			}
		});
	}

	public SetFeatureColorAction(IStructuredSelection selection, IFeatureModelManager featureModelManager) {
		this(featureModelManager);
		updateFeatureList(selection);
	}

	private SetFeatureColorAction(IFeatureModelManager featureModelManager) {
		super(COLORATION);
		setId(ID);
		setImageDescriptor(colorImage);
		this.featureModelManager = featureModelManager;
		setEnableUndoRedo(true);
	}

	public void setEnableUndoRedo(boolean set) {
		undoRedoEnabled = set;
	}

	private boolean isSelectionValid(IStructuredSelection selection) {
		if (featureModelManager != null) {
			final IFeatureModel featureModel = featureModelManager.getSnapshot();
			for (final Object object : selection.toList()) {
				if (object instanceof IFeature) {
					continue;
				} else if (object instanceof AbstractGraphicalEditPart) {
					final AbstractGraphicalEditPart agep = (AbstractGraphicalEditPart) object;
					IFeature feature = null;
					if ((agep.getModel() != null) && (agep.getModel() instanceof IGraphicalFeature)) {
						feature = featureModel.getFeature(agep.getModel().toString());
					}
					if (feature != null) {
						continue;
					}
				} else if (object instanceof FeatureEditPart) {
					continue;
				} else {
					return false;
				}
			}
		}
		return true;
	}

	public IFeatureModelManager getFeatureModelManager() {
		return featureModelManager;
	}

	public void setFeatureModelManager(IFeatureModelManager featureModelManager) {
		this.featureModelManager = featureModelManager;
		featureList.clear();
		setEnabled(false);
	}

	/**
	 * Creates a featureList with the selected features of the feature diagram.
	 *
	 * @param selection current selection
	 */
	public void updateFeatureList(IStructuredSelection selection) {
		if ((featureModelManager != null) && !selection.isEmpty()) {
			final IFeatureModel featureModel = featureModelManager.getSnapshot();
			featureList.clear();
			final Object[] editPartArray = selection.toArray();

			for (int i = 0; i < selection.size(); i++) {
				final Object editPart = editPartArray[i];

				if (editPart instanceof IFeature) {
					featureList.add((IFeature) editPart);
				} else if (editPart instanceof FeatureEditPart) {
					final FeatureEditPart editP = (FeatureEditPart) editPart;
					final IGraphicalFeature feature = editP.getModel();
					featureList.add(feature.getObject());
				} else if (editPart instanceof AbstractGraphicalEditPart) {
					final AbstractGraphicalEditPart agep = (AbstractGraphicalEditPart) editPart;
					final IFeature feature = featureModel.getFeature(agep.getModel().toString());
					featureList.add(feature);
				}
			}
		}
		setEnabled(!featureList.isEmpty());
	}

	// TODO implement as operation
	@Override
	public void run() {
		if (featureModelManager != null) {
			FeatureColor selectedColor = null;
			final Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			final List<IFeature> features = new ArrayList<>(featureList);

			if (!features.isEmpty()) {
				final IFeatureModel featureModel = featureModelManager.getSnapshot();
				if (featureModel != null) {
					// only allow coloration if the active profile is not the default profile
					if (FeatureColorManager.isDefault(featureModel)) {
						// skip color scheme wizard if there is only one color scheme available
						if (FeatureColorManager.getColorSchemes(featureModel).size() == 1) {
							final Wizard colorSchemeWizard = new ColorSchemeWizard(featureModel);
							final WizardDialog dialog = new WizardDialog(shell, colorSchemeWizard);
							dialog.create();
							final int dialogExitCode = dialog.open();
							if (dialogExitCode == Window.CANCEL) {
								return;
							} else if ((dialogExitCode == Window.OK) && FeatureColorManager.getCurrentColorScheme(featureModel).isDefault()) {
								MessageDialog.openError(shell, StringTable.CURRENTLY_NO_COLOR_SCHEME_SELECTED,
										StringTable.CURRENTLY_NO_COLOR_SCHEME_SELECTED_DIALOG);
								return;
							}
						}
						if (FeatureColorManager.getColorSchemes(featureModel).size() == 2) {
							// if there is one non-default color scheme, set it active
							final Collection<ColorScheme> colorSchemes = FeatureColorManager.getColorSchemes(featureModel);
							for (final ColorScheme colorScheme : colorSchemes) {
								if (!colorScheme.isDefault()) {
									FeatureColorManager.setActive(featureModel, colorScheme.getName());
								}
							}
						}
					}
				}

				// If the color of only one object should be changed, its color is selected in the dialog initially.
				if (features.size() == 1) {
					final IFeature selectedFeature = features.get(0);
					selectedColor = FeatureColorManager.getColor(selectedFeature);
				}

				final SetFeatureColorDialog dialog =
					new SetFeatureColorDialog(shell, featureModelManager, Functional.mapToStringList(features), selectedColor, undoRedoEnabled);

				// inform ui to update
				if (dialog.open() == Window.OK) {
					final IProject project = EclipseFileSystem.getResource(featureModel.getSourceFile()).getProject();
					try {
						project.touch(null);
						project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
					} catch (final CoreException e) {
						FMUIPlugin.getDefault().logError(e);
					}
				}
			}
		}
	}

}
