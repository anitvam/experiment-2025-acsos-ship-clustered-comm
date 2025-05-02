import numpy as np
import xarray as xr
import re
from pathlib import Path
import collections

def distance(val, ref):
    return abs(ref - val)
vectDistance = np.vectorize(distance)

def cmap_xmap(function, cmap):
    """ Applies function, on the indices of colormap cmap. Beware, function
    should map the [0, 1] segment to itself, or you are in for surprises.

    See also cmap_xmap.
    """
    cdict = cmap._segmentdata
    function_to_map = lambda x : (function(x[0]), x[1], x[2])
    for key in ('red','green','blue'):
        cdict[key] = map(function_to_map, cdict[key])
#        cdict[key].sort()
#        assert (cdict[key][0]<0 or cdict[key][-1]>1), "Resulting indices extend out of the [0, 1] segment."
    return matplotlib.colors.LinearSegmentedColormap('colormap',cdict,1024)

def getClosest(sortedMatrix, column, val):
    while len(sortedMatrix) > 3:
        half = int(len(sortedMatrix) / 2)
        sortedMatrix = sortedMatrix[-half - 1:] if sortedMatrix[half, column] < val else sortedMatrix[: half + 1]
    if len(sortedMatrix) == 1:
        result = sortedMatrix[0].copy()
        result[column] = val
        return result
    else:
        safecopy = sortedMatrix.copy()
        safecopy[:, column] = vectDistance(safecopy[:, column], val)
        minidx = np.argmin(safecopy[:, column])
        safecopy = safecopy[minidx, :].A1
        safecopy[column] = val
        return safecopy

def convert(column, samples, matrix):
    return np.matrix([getClosest(matrix, column, t) for t in samples])

def valueOrEmptySet(k, d):
    return (d[k] if isinstance(d[k], set) else {d[k]}) if k in d else set()

def mergeDicts(d1, d2):
    """
    Creates a new dictionary whose keys are the union of the keys of two
    dictionaries, and whose values are the union of values.

    Parameters
    ----------
    d1: dict
        dictionary whose values are sets
    d2: dict
        dictionary whose values are sets

    Returns
    -------
    dict
        A dict whose keys are the union of the keys of two dictionaries,
    and whose values are the union of values

    """
    res = {}
    for k in d1.keys() | d2.keys():
        res[k] = valueOrEmptySet(k, d1) | valueOrEmptySet(k, d2)
    return res

def extractCoordinates(filename):
    """
    Scans the header of an Alchemist file in search of the variables.

    Parameters
    ----------
    filename : str
        path to the target file
    mergewith : dict
        a dictionary whose dimensions will be merged with the returned one

    Returns
    -------
    dict
        A dictionary whose keys are strings (coordinate name) and values are
        lists (set of variable values)

    """
    with open(filename, 'r') as file:
#        regex = re.compile(' (?P<varName>[a-zA-Z._-]+) = (?P<varValue>[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?),?')
        regex = r"(?P<varName>[a-zA-Z._-]+) = (?P<varValue>[^,]*),?"
        dataBegin = r"\d"
        is_float = r"[-+]?\d*\.?\d+(?:[eE][-+]?\d+)?"
        for line in file:
            match = re.findall(regex, line.replace('Infinity', '1e30000'))
            if match:
                return {
                    var : float(value) if re.match(is_float, value)
                        else bool(re.match(r".*?true.*?", value.lower())) if re.match(r".*?(true|false).*?", value.lower())
                        else value
                    for var, value in match
                }
            elif re.match(dataBegin, line[0]):
                return {}

def extractVariableNames(filename):
    """
    Gets the variable names from the Alchemist data files header.

    Parameters
    ----------
    filename : str
        path to the target file

    Returns
    -------
    list of list
        A matrix with the values of the csv file

    """
    with open(filename, 'r') as file:
        dataBegin = re.compile('\d')
        lastHeaderLine = ''
        for line in file:
            if dataBegin.match(line[0]):
                break
            else:
                lastHeaderLine = line
        if lastHeaderLine:
            regex = re.compile(' (?P<varName>\S+)')
            return regex.findall(lastHeaderLine)
        return []

def openCsv(path):
    """
    Converts an Alchemist export file into a list of lists representing the matrix of values.

    Parameters
    ----------
    path : str
        path to the target file

    Returns
    -------
    list of list
        A matrix with the values of the csv file

    """
    regex = re.compile('\d')
    with open(path, 'r') as file:
        lines = filter(lambda x: regex.match(x[0]), file.readlines())
        return [[float(x) for x in line.split()] for line in lines]

def beautifyValue(v):
    """
    Converts an object to a better version for printing, in particular:
        - if the object converts to float, then its float value is used
        - if the object can be rounded to int, then the int value is preferred

    Parameters
    ----------
    v : object
        the object to try to beautify

    Returns
    -------
    object or float or int
        the beautified value
    """
    try:
        v = float(v)
        if v.is_integer():
            return int(v)
        return v
    except:
        return v

if __name__ == '__main__':
    # CONFIGURE SCRIPT
    # Where to find Alchemist data files
    directory = 'data'
    # Where to save charts
    output_directory = 'charts'
    # How to name the summary of the processed data
    pickleOutput = 'data_summary'
    # Experiment prefixes: one per experiment (root of the file name)
    experiments = ['clustered_comm']
    floatPrecision = '{: 0.3f}'
    # Number of time samples 
    timeSamples = 200
    # time management
    minTime = 0
    maxTime = 21600
    timeColumnName = 'time'
    logarithmicTime = False

    # One or more variables are considered random and "flattened"
    seedVars = ['seed', 'longseed']
    # Label mapping
    class Measure:
        def __init__(self, description, unit = None):
            self.__description = description
            self.__unit = unit
        def description(self):
            return self.__description
        def unit(self):
            return '' if self.__unit is None else f'({self.__unit})'
        def derivative(self, new_description = None, new_unit = None):
            def cleanMathMode(s):
                return s[1:-1] if s[0] == '$' and s[-1] == '$' else s
            def deriveString(s):
                return r'$d ' + cleanMathMode(s) + r'/{dt}$'
            def deriveUnit(s):
                return f'${cleanMathMode(s)}' + '/{s}$' if s else None
            result = Measure(
                new_description if new_description else deriveString(self.__description),
                new_unit if new_unit else deriveUnit(self.__unit),
            )
            return result
        def __str__(self):
            return f'{self.description()} {self.unit()}'
    
    centrality_label = 'H_a(x)'
    def expected(x):
        return r'\mathbf{E}[' + x + ']'
    def stdev_of(x):
        return r'\sigma{}[' + x + ']'
    def mse(x):
        return 'MSE[' + x + ']'
    def cardinality(x):
        return r'\|' + x + r'\|'

    labels = {
        'reduction-factor[mean]': Measure('ReductionFactor := sum(inter-cluster)/sum(inter-cluster)'),
        'reduction-factor[median]': Measure('ReductionFactor := sum(inter-cluster)/sum(inter-cluster)'),
        'reduction-factor[StandardDeviation]': Measure('ReductionFactor := sum(inter-cluster)/sum(inter-cluster)'),
        
        # 'export-intra-cluster-relay-data-rate-not-leader[mean]': Measure('Intra-Cluster Data Rate'),
        # 'export-intra-cluster-relay-data-rate-not-leader[variance]': Measure('Intra-Cluster Data Rate'),
        # 'export-intra-cluster-relay-data-rate-not-leader[median]': Measure('Intra-Cluster Data Rate'),
        # 'leader-to-relay-data-rate[mean]': Measure('Inter-Cluster Data Rate'),
        # 'leader-to-relay-data-rate[variance]': Measure('Inter-Cluster Data Rate'),
        # 'leader-to-relay-data-rate[median]': Measure('Inter-Cluster Data Rate'),
    }
    def derivativeOrMeasure(variable_name):
        if variable_name.endswith('dt'):
            return labels.get(variable_name[:-2], Measure(variable_name)).derivative()
        return Measure(variable_name)
    def label_for(variable_name):
        return labels.get(variable_name, derivativeOrMeasure(variable_name)).description()
    def unit_for(variable_name):
        return str(labels.get(variable_name, derivativeOrMeasure(variable_name)))
    
    # Setup libraries
    np.set_printoptions(formatter={'float': floatPrecision.format})
    # Read the last time the data was processed, reprocess only if new data exists, otherwise just load
    import pickle
    import os
    if os.path.exists(directory):
        newestFileTime = max([os.path.getmtime(directory + '/' + file) for file in os.listdir(directory)], default=0.0)
        try:
            lastTimeProcessed = pickle.load(open('timeprocessed', 'rb'))
        except:
            lastTimeProcessed = -1
        shouldRecompute = not os.path.exists(".skip_data_process") and newestFileTime != lastTimeProcessed
        if not shouldRecompute:
            try:
                means = pickle.load(open(pickleOutput + '_mean', 'rb'))
                stdevs = pickle.load(open(pickleOutput + '_std', 'rb'))
            except:
                shouldRecompute = True
        if shouldRecompute:
            timefun = np.logspace if logarithmicTime else np.linspace
            means = {}
            stdevs = {}
            for experiment in experiments:
                # Collect all files for the experiment of interest
                import fnmatch
                allfiles = filter(lambda file: fnmatch.fnmatch(file, experiment + '_*.csv'), os.listdir(directory))
                #print(allfiles)
                allfiles = [directory + '/' + name for name in allfiles]
                #print(allfiles)
                allfiles.sort()
                # From the file name, extract the independent variables
                dimensions = {}
                for file in allfiles:
                    dimensions = mergeDicts(dimensions, extractCoordinates(file))
                dimensions = {k: sorted(v) for k, v in dimensions.items()}
                # Add time to the independent variables
                dimensions[timeColumnName] = range(0, timeSamples)
                # Compute the matrix shape
                shape = tuple(len(v) for k, v in dimensions.items())
                # Prepare the Dataset
                dataset = xr.Dataset()
                for k, v in dimensions.items():
                    dataset.coords[k] = v
                if len(allfiles) == 0:
                    print("WARNING: No data for experiment " + experiment)
                    means[experiment] = dataset
                    stdevs[experiment] = xr.Dataset()
                else:
                    varNames = extractVariableNames(allfiles[0])
                    for v in varNames:
                        if v != timeColumnName:
                            novals = np.ndarray(shape)
                            novals.fill(float('nan'))
                            dataset[v] = (dimensions.keys(), novals)
                    # Compute maximum and minimum time, create the resample
                    timeColumn = varNames.index(timeColumnName)
                    allData = { file: np.matrix(openCsv(file)) for file in allfiles }
                    computeMin = minTime is None
                    computeMax = maxTime is None
                    if computeMax:
                        maxTime = float('-inf')
                        for data in allData.values():
                            maxTime = max(maxTime, data[-1, timeColumn])
                    if computeMin:
                        minTime = float('inf')
                        for data in allData.values():
                            minTime = min(minTime, data[0, timeColumn])
                    timeline = timefun(minTime, maxTime, timeSamples)
                    # Resample
                    for file in allData:
    #                    print(file)
                        allData[file] = convert(timeColumn, timeline, allData[file])
                    # Populate the dataset
                    for file, data in allData.items():
                        dataset[timeColumnName] = timeline
                        for idx, v in enumerate(varNames):
                            if v != timeColumnName:
                                darray = dataset[v]
                                experimentVars = extractCoordinates(file)
                                darray.loc[experimentVars] = data[:, idx].A1
                    # Fold the dataset along the seed variables, producing the mean and stdev datasets
                    mergingVariables = [seed for seed in seedVars if seed in dataset.coords]
                    means[experiment] = dataset.mean(dim = mergingVariables, skipna=True)
                    stdevs[experiment] = dataset.std(dim = mergingVariables, skipna=True)
            # Save the datasets
            pickle.dump(means, open(pickleOutput + '_mean', 'wb'), protocol=-1)
            pickle.dump(stdevs, open(pickleOutput + '_std', 'wb'), protocol=-1)
            pickle.dump(newestFileTime, open('timeprocessed', 'wb'))
    else:
        means = { experiment: xr.Dataset() for experiment in experiments }
        stdevs = { experiment: xr.Dataset() for experiment in experiments }

    # QUICK CHARTING

    import matplotlib
    import matplotlib.pyplot as plt
    import matplotlib.cm as cmx
    matplotlib.rcParams.update({'axes.titlesize': 12})
    matplotlib.rcParams.update({'axes.labelsize': 10})
    
    def make_line_chart(
        xdata,
        ydata,
        title=None,
        ylabel=None,
        xlabel=None,
        colors=None,
        linewidth=1,
        error_alpha=0.2,
        figure_size=(6, 4)
    ):
        fig = plt.figure(figsize = figure_size)
        ax = fig.add_subplot(1, 1, 1)
        ax.set_title(title)
        ax.set_xlabel(xlabel)
        ax.set_ylabel(ylabel)
#        ax.set_ylim(0)
#        ax.set_xlim(min(xdata), max(xdata))
        index = 0
        for (label, (data, error)) in ydata.items():
            print(f'plotting {data}\nagainst {xdata}')
            lines = ax.plot(xdata, data, label=label, color=colors(index / (len(ydata) - 1)) if colors else None, linewidth=linewidth)
            index += 1
            if error is not None:
                last_color = lines[-1].get_color()
                ax.fill_between(
                    xdata,
                    data+error,
                    data-error,
                    facecolor=last_color,
                    alpha=error_alpha,
                )
        return (fig, ax)
    def generate_all_charts(means, errors = None, basedir=''):
        viable_coords = { coord for coord in means.coords if means[coord].size > 1 }
        for comparison_variable in viable_coords - {timeColumnName}:
            mergeable_variables = viable_coords - {timeColumnName, comparison_variable}
            for current_coordinate in mergeable_variables:
                merge_variables = mergeable_variables - { current_coordinate }
                merge_data_view = means.mean(dim = merge_variables, skipna = True)
                merge_error_view = errors.mean(dim = merge_variables, skipna = True)
                for current_coordinate_value in merge_data_view[current_coordinate].values:
                    beautified_value = beautifyValue(current_coordinate_value)
                    for current_metric in merge_data_view.data_vars:
                        title = f'{label_for(current_metric)} for diverse {label_for(comparison_variable)} when {label_for(current_coordinate)}={beautified_value}'
                        for withErrors in [True, False]:
                            fig, ax = make_line_chart(
                                title = title,
                                xdata = merge_data_view[timeColumnName],
                                xlabel = unit_for(timeColumnName),
                                ylabel = unit_for(current_metric),
                                ydata = {
                                    beautifyValue(label): (
                                        merge_data_view.sel(selector)[current_metric],
                                        merge_error_view.sel(selector)[current_metric] if withErrors else 0
                                    )
                                    for label in merge_data_view[comparison_variable].values
                                    for selector in [{comparison_variable: label, current_coordinate: current_coordinate_value}]
                                },
                            )
                            ax.set_xlim(minTime, maxTime)
                            ax.legend()
                            fig.tight_layout()
                            by_time_output_directory = f'{output_directory}/{basedir}/{comparison_variable}'
                            Path(by_time_output_directory).mkdir(parents=True, exist_ok=True)
                            figname = f'{comparison_variable}_{current_metric}_{current_coordinate}_{beautified_value}{"_err" if withErrors else ""}'
                            for symbol in r".[]\/@:":
                                figname = figname.replace(symbol, '_')
                            fig.savefig(f'{by_time_output_directory}/{figname}.pdf')
                            plt.close(fig)
    for experiment in experiments:
        current_experiment_means = means[experiment]
        current_experiment_errors = stdevs[experiment]
        #generate_all_charts(current_experiment_means, current_experiment_errors, basedir = f'{experiment}/all')
    
# Custom charting
    from matplotlib.gridspec import SubplotSpec
    from datetime import datetime, timedelta
    import pandas as pd
    import pytz
    
    def compute_dimensions():
        dimensions = {}
        import fnmatch
        allfiles = filter(lambda file: fnmatch.fnmatch(file, experiment + '_*.csv'), os.listdir(directory))
        allfiles = [directory + '/' + name for name in allfiles]
        allfiles.sort()
        for file in allfiles:
            dimensions = mergeDicts(dimensions, extractCoordinates(file))
        return dimensions
        
    def save_fig(fig, plt, name): 
        fig.tight_layout()
        Path(output_directory).mkdir(parents=True, exist_ok=True)
        fig.savefig(f'{output_directory}/{name}.pdf')
        plt.close(fig)

    def seconds_to_datetime(seconds):
        """
        Converts seconds to datetime starting from August 18, 2022, 6:00 AM GMT+2 (with DST).
        
        Parameters:
            seconds (int or float): Number of seconds since the start time.
    
        Returns:
            datetime: Resulting datetime object in Europe/Berlin timezone (with DST handled).
        """
        # Define the timezone (DST-aware)
        tz = pytz.timezone('Europe/Berlin')  # GMT+02:00 in summer (DST)
    
        # Define the start datetime in local time
        start_time = tz.localize(datetime(2022, 8, 18, 6, 0, 0))
        # Add the seconds
        result_time = start_time + timedelta(seconds=seconds)
        formatted = result_time.strftime("%H:%M")
        return formatted
    
    def custom_linechart_subplot(ax, ds, errors, evaluatingColumn, values, algorithm, color_value, isLastColumn=False):
       #evaluatingValues = ds.coords[evaluatingColumn].values
       viridis = plt.colormaps['viridis']
       for idx, x in enumerate(values):
           dataset = ds.sel({"g-probability": x}).to_dataframe()
           errorsDataset = errors.sel({"g-probability": x}).to_dataframe()
           sigmaMinus = dataset[evaluatingColumn] - errorsDataset[evaluatingColumn]
           sigmaPlus = dataset[evaluatingColumn] + errorsDataset[evaluatingColumn]
           ax[idx].plot(ds[timeColumnName], dataset[evaluatingColumn], label=algorithm, color=viridis(color_value), linewidth=2.0)
           ax[idx].fill_between(ds[timeColumnName], sigmaMinus, sigmaPlus, color=viridis(color_value), alpha=0.2)
           if isLastColumn:
               ax[idx].set_xlabel('Time ($ s $)')
           ax[idx].set_xlim(0, 21600)
           ax[idx].set_ylim(-0.1, None)
           #ax[idx].set_yscale('symlog', linthresh=10)
           #ax[idx].set_ylabel('Squared Distance Error ($ m^2 $)')
           ax[idx].set_title('$p_{5G}$' + f'= {x}')
           #ax[idx].legend()
           #ax[idx].margins(x=0)
           ticks = np.arange(0.0, 21601, 3600)
           custom_labels = [seconds_to_datetime(x) for x in ticks]
           ax[idx].set_xticks(ticks)
           ax[idx].set_xticklabels(custom_labels)  
    
    def linechart_datarate(means, errors):
        dimensions = compute_dimensions()
        updateFreq = dimensions['update-frequency']
        gprob = dimensions['g-probability']
        gprob = list(gprob)
        gprob.sort()
        updateFreq=[min(updateFreq), max(updateFreq)]
        gprob = ['0.0', '0.01', '0.05']
        fig, axes = plt.subplots(2, len(gprob), figsize=(11, 5), sharey=False, layout="constrained")
        axes[0][0].set_ylabel(f'Update Freq. = {'{0:.4f}'.format(updateFreq[0])}Hz \n Data Rate (Kbps)')
        axes[1][0].set_ylabel(f'Update Freq. = {'{0:.1f}'.format(updateFreq[1])}Hz \n Data Rate (Kbps)')
        for idf, freq in enumerate(updateFreq): 
            custom_linechart_subplot(axes[idf], means.sel({"update-frequency": freq }), errors.sel({"update-frequency": freq }), 'bcdr[mean]', gprob, 'Mean $CSC$ $DR$', 0.25, idf+1==len(updateFreq)) #0.25*(idf+1.5)
            custom_linechart_subplot(axes[idf], means.sel({"update-frequency": freq }), errors.sel({"update-frequency": freq }), 'b1dr[mean]', gprob, 'Mean $baseline$ $DR$', 0.5, idf+1==len(updateFreq)) #0.5
            custom_linechart_subplot(axes[idf], means.sel({"update-frequency": freq }), errors.sel({"update-frequency": freq }), 'b2dr[mean]', gprob, 'Mean $Dist-MR$ $DR$', 0.75, idf+1==len(updateFreq)) #0.75
            custom_linechart_subplot(axes[idf], means.sel({"update-frequency": freq }), errors.sel({"update-frequency": freq }), 'b3dr[mean]', gprob, 'Mean $DR-MR$ $DR$', 0.9, idf+1==len(updateFreq)) #0.9
        
        for idf, f in enumerate(updateFreq): 
            for idp, p in enumerate(gprob):
                axes[idf][idp].set_ylim(1000, 3000)
        
        axes[0][0].legend()
         
        save_fig(fig, plt, 'linechart_datarate')
        
    def linechart_clusteredmetrics(means, errors, metric, label, lowLim, upLim):
        dimensions = compute_dimensions()
        updateFreq = [0.2]
        freq = 0.2
        gprob = dimensions['g-probability']
        fig, axes = plt.subplots(1, 1, figsize=(10, 5), sharey=False, layout="constrained")
        viridis = plt.colormaps['viridis']
        gprob = list(gprob)
        gprob.sort()
        # for idp, prob in enumerate(gprob): 
        #    custom_linechart_subplot(axes, means.sel({"update-frequency": freq }), errors.sel({"update-frequency": freq }), metric, updateFreq, label, 0.1, True)
        ds = means.sel({"update-frequency": freq })
        errors = errors.sel({"update-frequency": freq })
        for idx, x in enumerate(gprob):
            dataset = ds.sel({"g-probability": x}).to_dataframe()
            errorsDataset = errors.sel({"g-probability": x}).to_dataframe()
            sigmaMinus = dataset[metric] - errorsDataset[metric]
            sigmaPlus = dataset[metric] + errorsDataset[metric]
            vcolor = viridis(0.15+(idx*0.13))
            axes.plot(ds[timeColumnName], dataset[metric], label=f'$p_{'5G'}$ = {x}', color=vcolor, linewidth=2.0)
            axes.fill_between(ds[timeColumnName], sigmaMinus, sigmaPlus, color=vcolor, alpha=0.2)
        
        axes.set_xlabel('Time ($ s $)')
        axes.set_xlim(0, 21600)
        axes.set_ylim(-0.1, None)
        #ax[idx].set_yscale('symlog', linthresh=10)
        #ax[idx].set_ylabel('Squared Distance Error ($ m^2 $)')
        axes.set_title(label)
        #ax[idx].legend()
        #ax[idx].margins(x=0)
        ticks = np.arange(0.0, 21601, 3600)
        custom_labels = [seconds_to_datetime(x) for x in ticks]
        axes.set_xticks(ticks)
        axes.set_xticklabels(custom_labels)
        
        axes.set_ylim(lowLim, upLim)
        
        axes.legend()
        
        save_fig(fig, plt, "linechart_"+metric)
        
    def barchart_clusteredmetrics(means, errors, metric, label, lowLim, upLim):
        dimensions = compute_dimensions()
        updateFreq = [0.2]
        freq = 0.2
        gprob = dimensions['g-probability']
        fig, axes = plt.subplots(1, 1, figsize=(10, 5), sharey=False, layout="constrained")
        viridis = plt.colormaps['viridis']
        gprob = list(gprob)
        gprob.sort()
        
        bar_width = 0.9
        x = gprob
        x_pos = np.arange(len(x))
        ds = means.sel({"update-frequency": freq })
        errors = errors.sel({"update-frequency": freq })
        
        average = []
        std_dev = []
        for idx, x in enumerate(gprob):
            data = ds.sel({"g-probability": x})[metric]
            data = data.isel(time=slice(1, None)) #Drop 1st value
            average.append(np.mean(data))
            std_dev.append(np.std(data))
            
        vcolor = viridis(0.5)
        axes.set_xlim(x_pos[0]-0.5, x_pos[-1]+0.5)
        axes.bar(x_pos, average, width=bar_width, yerr=std_dev, label=f'$p_{'5G'}$ = {x}', color=vcolor)
        axes.set_xticks(x_pos, gprob)        
        axes.set_xlabel(f'$p_{'5G'}$')
        axes.set_title(label)
        axes.set_ylim(lowLim, upLim)
        save_fig(fig, plt, "barchart_"+metric)
        
    def custom_barchart_subplot(ax, ds, errors, freq, probabilities, color_value):  
       evaluatingColumns = {
           "bcdr[mean]": 'Mean $CSC$ $DR$', 
           "b1dr[mean]": 'Mean $baseline$ $DR$', 
           "b2dr[mean]": 'Mean $Dist-MR$ $DR$',
           "b3dr[mean]": 'Mean $DR-MR$ $DR$'
       }
       viridis = plt.colormaps['viridis']
       bar_width = 0.2
       number_of_comparisons = len(evaluatingColumns)
       range_comparisons = np.arange(number_of_comparisons)
       x = probabilities
       x_pos = np.arange(len(x))
       for i, (key, value) in enumerate(evaluatingColumns.items()):
           average = []
           std_dev = []
           for idp, prob in enumerate(probabilities):
               data = ds.sel({"g-probability": prob})[key]
               data = data.isel(time=slice(1, None)) #Drop 1st value
               average.append(np.mean(data))
               std_dev.append(np.std(data))
           offset = (i - number_of_comparisons / 2) * bar_width + bar_width / 2
           ax.set_xlim(x_pos[0]-(2*bar_width), x_pos[-1]+(2*bar_width))
           ax.bar(x_pos + offset, average, bar_width, yerr=std_dev, label=value, color=viridis(0.25*(i+1.5)))
           ax.set_xticks(x_pos, probabilities)
           #ax.legend()
           
          
           
    def barchart_datarate(means, errors):
        dimensions = compute_dimensions()
        updateFreq = list(dimensions['update-frequency'])
        gprob = dimensions['g-probability']
        gprob = list(gprob)
        gprob.sort()
        fig, axes = plt.subplots(len(updateFreq), 1, figsize=(8, 8), sharey=False, layout="constrained")
        axes[len(updateFreq)-1].set_xlabel("$p_{5G}$")
        
        for idf, freq in enumerate(updateFreq): 
            custom_barchart_subplot(axes[idf], means.sel({"update-frequency": freq }), errors.sel({"update-frequency": freq }), freq, gprob, 0.25*(idf+1.5))    
        axes[len(updateFreq)-1].legend(loc='lower right', framealpha=0.95)
        axes[0].set_ylabel(f'Update Freq. = {'{0:.4f}'.format(updateFreq[0])}Hz\nData Rate (Kbps)')
        axes[1].set_ylabel(f'Update Freq. = {'{0:.1f}'.format(updateFreq[1])}Hz\nData Rate (Kbps)')
        axes[2].set_ylabel(f'Update Freq. = {'{0:.1f}'.format(updateFreq[2])}Hz\nData Rate (Kbps)')
        
        
        save_fig(fig, plt,'barchart_datarate')
        
        
        
    def plot_metric_chart():
        df = pd.read_csv("data/metric_data.csv")
        viridis = plt.colormaps['viridis']
        
        plt.figure(figsize=(10, 7))
        widthOfLine = 4
        plt.plot(df["x"], df["y_wifi"], label="Wi-Fi", linewidth=widthOfLine, color=viridis(0.2))
        plt.plot(df["x"], df["y_aprs"], label="APRS", linewidth=widthOfLine, color=viridis(0.4))
        plt.plot(df["x"], df["y_lora"], label="LoraWAN", linewidth=widthOfLine, color=viridis(0.6))
        plt.plot(df["x"], df["y_midband5g"], label="Midband 5G", linewidth=widthOfLine, color=viridis(8))
        
        def addHorizontalAxe(yValue, label, position=10000):
            plt.axhline(y=yValue, color='black', linestyle='--')
            plt.text(x=position, y=yValue + 0.00000005, s=label, color='black', fontsize=10, verticalalignment='bottom' )
        
        addHorizontalAxe(5.0, "Full-HD video (~5Mbps)")
        addHorizontalAxe(2.5, "HD-Ready audio (~2.5Mbps)")
        addHorizontalAxe(0.320, "High-Quality audio (~320kbps)")
        addHorizontalAxe(0.064, "Low-Quality audio (~64kbps)")
        addHorizontalAxe(0.032, "Speech only audio (~32kbps)")
        addHorizontalAxe(0.100, "Rich text data (~100kbps)")
        addHorizontalAxe(0.001, "Position, Identification, Direction (1kbps)", 1.5)
        addHorizontalAxe(0.0001, "Keep Alive Message (10bps)", 1.5)
                
        plt.yscale('symlog', linthresh=0.001)
        plt.xscale("log")
        plt.legend(loc="upper right")
        plt.xlim(1.0, 60000.0)
        plt.ylim(-0.0001, 10000.0)
        plt.xlabel("Distance (meters)")
        plt.ylabel("Data Rate (Mbps)")
        plt.tight_layout()
        Path(output_directory).mkdir(parents=True, exist_ok=True)
        plt.savefig(f'{output_directory}/metric_chart.pdf')

    
    
    linechart_datarate(means[experiment], stdevs[experiment] )
    linechart_clusteredmetrics(means[experiment], stdevs[experiment], 'n_clusters',  'Average Number of Clusters', None, None)
    linechart_clusteredmetrics(means[experiment], stdevs[experiment], 'cluster-size[mean]',  'Average Cluster Size', None, None)
    linechart_clusteredmetrics(means[experiment], stdevs[experiment], 'clustersComposedOfOneElement',  'Number of Clusters composed of one element', None, None)
    linechart_clusteredmetrics(means[experiment], stdevs[experiment], 'reduction-factor[mean]',  'Average Reduction Factor', 0.7, 1)
    barchart_clusteredmetrics(means[experiment], stdevs[experiment], 'n_clusters',  'Average Number of Clusters', None, None)
    barchart_clusteredmetrics(means[experiment], stdevs[experiment], 'cluster-size[mean]',  'Average Cluster Size', None, None)
    barchart_clusteredmetrics(means[experiment], stdevs[experiment], 'clustersComposedOfOneElement',  'Number of Clusters composed of one element', None, None)
    barchart_clusteredmetrics(means[experiment], stdevs[experiment], 'reduction-factor[mean]',  'Average Reduction Factor', 0.7, 1)
    barchart_datarate(means[experiment], stdevs[experiment])
    plot_metric_chart()
    
    
        
 
    