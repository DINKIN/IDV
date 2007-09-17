
"""A set of utilities for selecting data nad creating displays.
   For use from the Jython shell."""


def selectData(name):
    list = java.util.ArrayList();
    list.add(name);
    result = idv.selectDataChoices(list);
    if(result == None): 
	return None;
    return result.get(0).getData(None);

def selectDataChoice(name='Select Field'):
    list = java.util.ArrayList();
    list.add(name);
    result = idv.selectDataChoices(list);
    if(result == None): 
	return None;
    return result.get(0);


def createDisplay(displayType, data, dataName='Data'):
	import ucar.unidata.data.DataDataChoice as DataDataChoice
       	import ucar.unidata.data.DataChoice as DataChoice
	if(isinstance(data, DataChoice)==0):
             data = DataDataChoice(dataName,data);
	return idv.doMakeControl(displayType, data);


#def selectedData(name):
#    list = ArrayList();
#    list.add(name);
#    result = idv.selectDataChoices(list);
#    if(result == None) return None;
#    return result.get(0);


