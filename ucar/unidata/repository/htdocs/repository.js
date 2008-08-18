



function Util () {

    this.loadXML = function (url, callback,arg) {
        var req = false;
        if(window.XMLHttpRequest) {
            try {
                req = new XMLHttpRequest();
            } catch(e) {
                req = false;
            }
        } else if(window.ActiveXObject)  {
            try {
                req = new ActiveXObject("Msxml2.XMLHTTP");
            } catch(e) {
                try {
                    req = new ActiveXObject("Microsoft.XMLHTTP");
                } catch(e) {
                    req = false;
                }
            }
        }
        if(req) {
            req.onreadystatechange = function () { 
                if (req.readyState == 4 && req.status == 200)   {
                    callback(req,arg); 
                }
            };
            req.open("GET", url, true);
            req.send("");
        }
    }



    this.setCursor = function(c) {
        var cursor = document.cursor;
        if(!cursor && document.getElementById) {
            cursor =  document.getElementById('cursor');
        }
        if(!cursor) {
            document.body.style.cursor = c;
        }
    }


    this.getDomObject = function(name) {
        obj = new DomObject(name);
        if(obj.obj) return obj;
        return null;
    }



    this.getKeyChar = function(event) {
        event = util.getEvent(event);
        if(event.keyCode) {
            return String.fromCharCode(event.keyCode);
        }
        if(event.which)  {
            return String.fromCharCode(event.which);
        }
        return '';
    }


    this.print = function (s, clear) {
        var obj = util.getDomObject("output");
        if(!obj) return;
        if(clear) {
            obj.obj.innerHTML  ="";
        }
        obj.obj.innerHTML  =obj.obj.innerHTML+"<br>" +s;
    }



    this.getEvent = function (event) {
        if(event) return event;
        return window.event;
    }


    this.getEventX =    function (event) {
        if (event.pageX) {
            return  event.pageX;
        }
        return  event.clientX + document.body.scrollLeft
        + document.documentElement.scrollLeft;
    }

    this.getEventY =function (event) {
        if (event.pageY) {
            return  event.pageY;
        }
        return  event.clientY + document.body.scrollTop
        + document.documentElement.scrollTop;

    }

    this.getTop = function (obj) {
        if(!obj) return 0;
        return obj.offsetTop+this.getTop(obj.offsetParent);
    }



    this.setPosition = function(obj,x,y) {
        obj.style.top = y;
        obj.style.left = x;
    }

    this.getLeft =  function(obj) {
        if(!obj) return 0;
        return obj.offsetLeft+this.getLeft(obj.offsetParent);
    }
}

util = new Util();



function DomObject(name) {
    this.obj = null;
// DOM level 1 browsers: IE 5+, NN 6+
    if (document.getElementById)	{    	
        this.obj = document.getElementById(name);
        if(this.obj) 
            this.style = this.obj.style;
    }
// IE 4
    else if (document.all)	{  			
        this.obj = document.all[name];
        if(this.obj) 
            this.style = this.obj.style;
    }
// NN 4
    else if (document.layers)  { 		
        this.obj = document.layers[name];
        this.style = document.layers[name];
    }
}



function noop() {
}







var menuObject;
document.onmousemove = mouseMove;
document.onmousedown = mouseDown;
document.onmouseup   = mouseUp;

var mouseIsDown = 0;
var draggedEntry;
var draggedEntryName;
var mouseMoveCnt =0;

function hideMenuObject() {
    if(menuObject) {
        hideObject(menuObject);
        menuObject = null;
    }
}


function mouseDown(event) {
    if(menuObject) {
        setTimeout("hideMenuObject()",1000);
    }
    event = util.getEvent(event);
    mouseIsDown = 1;
    mouseMoveCnt =0;
    return true;
}



function mouseUp(event) {
    event = util.getEvent(event);
    mouseIsDown = 0;
    draggedEntry   = null;
    util.setCursor('default');
    var obj = util.getDomObject('floatdiv');
    if(obj) {
        hideObject(obj);
    }
    return true;
}

function mouseMove(event) {
    event = util.getEvent(event);
    if(draggedEntry && mouseIsDown) {
        mouseMoveCnt++;
        var obj = util.getDomObject('floatdiv');
        if(mouseMoveCnt==6) {
            util.setCursor('move');
        }
        if(mouseMoveCnt>=6&& obj) {
            moveFloatDiv(util.getEventX(event),util.getEventY(event));
        }
    }    
    return false;
}


function moveFloatDiv(x,y) {
    var obj = util.getDomObject('floatdiv');
    if(obj) {
        if(obj.style.visibility!="visible") {
            obj.style.visibility = "visible";
            obj.style.display = "block";
            obj.obj.innerHTML = draggedEntryName;
        }
        obj.style.top = y;
        obj.style.left = x+10;
    }
}

function mouseOverOnEntry(event, id) {
    event = util.getEvent(event);
    if(id == draggedEntry) return;
    if(mouseIsDown)  {
        var obj = util.getDomObject("span_" + id);
        if(!obj)  return;
        //       if(obj.style && obj.style.borderBottom) {
        obj.style.borderBottom="2px black solid";
        //        }
    }
}

function mouseOutOnEntry(event, id) {
    event = util.getEvent(event);
    if(id == draggedEntry) return;
    var obj = util.getDomObject("span_" + id);
    if(!obj)  return;
    if(mouseIsDown)  {
        obj.style.borderBottom="";
    }
}




function mouseDownOnEntry(event, id, name) {
    event = util.getEvent(event);
    draggedEntry = id;
    draggedEntryName=name;
    mouseIsDown = 1;
    if(event.preventDefault) {
        event.preventDefault();
    } else {
	event.returnValue = false;
        return false;
    }
}


function mouseUpOnEntry(event, id) {
    event = util.getEvent(event);
    if(id == draggedEntry) return;
    var obj = util.getDomObject("span_" + id);
    if(!obj)  return;
    if(mouseIsDown)  {
        obj.style.borderBottom="";
    }
    if(draggedEntry && draggedEntry!=id) {
        url = "${urlroot}/entry/copy?action=action.move&from=" + draggedEntry +"&to=" + id;
        document.location = url;
    }
}





function setImage(id,url) {
    img = util.getDomObject(id);
    if(img) {
        img.obj.src  = url;
    }
}

function Tooltip () {
    var lastMove = 0;
    var needsToClose = 1;
    var showing = 0;
    var pinned = 0;

    this.unPin = function (event) {
        pinned = 0;
        needsToClose = 1;
        this.doHide();
    }

    this.keyPressed = function (event) {
        if(!showing) return;

        c =util.getKeyChar(event);
        if(c == '\r' && pinned) {
            tooltip.unPin(event);
        }
        if(c == 'p') {
            img = util.getDomObject('tooltipclose');
            msg = util.getDomObject('pindiv');
            hideObject(msg);
            if(img) {
                img.obj.src  = "${urlroot}/icons/close.gif";
                pinned = 1;
            }
        }

    }


    this.hide = function (event,id) {
        if(pinned) return;
        needsToClose = 1;
        showing = 0;
        tooltip.doHide();
    }


    this.doHide  = function() {
        if(!needsToClose || pinned) return;
        if(!showing)
            lastMove++;
        var obj = util.getDomObject("tooltipdiv");
        if(!obj) return;
        showing = 0;
        hideObject(obj);
    }


    this.show  = function(event,id) {
        if(showing) return;
        event = util.getEvent(event);
        lastMove++;
        setTimeout("tooltip.doShow(" + lastMove+"," +util.getEventX(event)+","+ util.getEventY(event) +"," + "'" + id +"'"+")", 1000);
    }


    this.divMouseOver = function() {
        //        needsToClose = 0;
    }

    this.divMouseOut = function() {
        //        needsToClose = 1;
        //        this.doHide();
    }

    this.doShow = function(moveId,x,y,id) {

        if(lastMove!=moveId) return;
        var link = util.getDomObject(id);
        if(link && link.obj.offsetLeft && link.obj.offsetWidth) {
            x= util.getLeft(link.obj);
            y = link.obj.offsetHeight+util.getTop(link.obj) + 2;
        } else {
            x+=20;
        }

        var obj = util.getDomObject("tooltipdiv");
        if(!obj) return;
        util.setPosition(obj, x,y);
        url = "${urlroot}/entry/show?id=" + id +"&output=metadataxml";
	util.loadXML( url, handleTooltip,obj);
    }


    function handleTooltip(request, obj) {
        var xmlDoc=request.responseXML.documentElement;
        text = getChildText(xmlDoc);
        obj.obj.innerHTML = "<div id=\"tooltipwrapper\" onmouseover=\"tooltip.divMouseOver();\"  onmouseout=\"tooltip.divMouseOut();\"><table><tr valign=top><img width=\"16\" onmousedown=\"tooltip.unPin();\" id=\"tooltipclose\" onmouseover=\"tooltip.divMouseOver();\" src=${urlroot}/icons/blank.gif></td><td>" + text+"</table><div id=\"pindiv\" class=smallmessage>'p' to pin</div></div>";
        showing = 1;
        showObject(obj);
    }

}

tooltip = new Tooltip();

document.onkeypress = tooltip.keyPressed;


function toggleEntryForm () {
    var obj = util.getDomObject('entryform');
    var img = util.getDomObject('entryformimg');
    if(obj) {
        if(toggleVisibilityOnObject(obj,'')) {
            if(img) img.obj.src =  "${urlroot}/icons/downarrow.gif";
        } else {
            if(img) img.obj.src =  "${urlroot}/icons/rightarrow.gif";
        }
    }
    var cnt = 0;
    while(1) {
        obj = util.getDomObject('entryform' + (cnt++));
        if(!obj) break;
        toggleVisibilityOnObject(obj,'');
    }
}




function toggleBlockVisibility(id, imgid, showimg, hideimg) {
    var img = util.getDomObject(imgid);
    if(toggleVisibility(id)) {
        if(img) img.obj.src = showimg;
    } else {
        if(img) img.obj.src = hideimg;
    }
}




function  handleFolderList(request, id) {
    var block = util.getDomObject("block_" + id);
    var img = util.getDomObject("img_" +id);
    var xmlDoc=request.responseXML.documentElement;
    block.obj.innerHTML = getChildText(xmlDoc);
    if(img) img.obj.src = "${urlroot}/icons/folderopen.gif";

}

function folderClick(id) {
    var block = util.getDomObject("block_" + id);
    if(!block) return;
    var img = util.getDomObject("img_" +id);
    if(!block.obj.isOpen) {
        block.obj.isOpen = 1;
        showObject(block);
        if(img) img.obj.src = "${urlroot}/icons/progress.gif";
        url = "${urlroot}/entry/show?id=" + id +"&output=groupxml";
	util.loadXML( url, handleFolderList,id);
    } else {
        if(img) img.obj.src = "${urlroot}/icons/folderclosed.gif";
        block.obj.isOpen = 0;
        hideObject(block);
    }
}











function  getChildText(xmlDoc) {
    var text = '';
    for(i=0;i<xmlDoc.childNodes.length;i++) {
        text = text  + xmlDoc.childNodes[i].nodeValue;
    }
    return text;
	
}



function toggleVisibility(id) {
    var obj = util.getDomObject(id);
    return toggleVisibilityOnObject(obj,'block');
}


function hide(id) {
    hideObject(util.getDomObject(id));
}


function showMenu(event,srcId,id) {
    var obj = util.getDomObject(id);
    var srcObj = util.getDomObject(srcId);
    if(!obj || !srcObj) return;
    event = util.getEvent(event);
    x = util.getEventX(event);
    y = util.getEventY(event);
    if(srcObj.obj.offsetLeft && srcObj.obj.offsetWidth) {
        x= util.getLeft(srcObj.obj);
        y = srcObj.obj.offsetHeight+util.getTop(srcObj.obj) + 2;
    } 

    x+=2;
    x+=3;

    menuObject = obj;
    showObject(obj);
    
    util.setPosition(obj, x,y);
}


function show(id) {
    showObject(util.getDomObject(id));
}

function hideObject(obj) {
    if(!obj) return 0;
    obj.style.visibility = "hidden";
    obj.style.display = "none";
}


function showObject(obj, display) {
    if(!obj) return 0;
    if(!display) display = "block";
    obj.style.visibility = "visible";
    obj.style.display = display;
    return 1;
}



function toggleVisibilityOnObject(obj, display) {
    if(!obj) return 0;
    if(obj.style.visibility == "hidden") {
        obj.style.visibility = "visible";
        obj.style.display = display;
        return 1;
    } else {
        obj.style.visibility = "hidden";
        obj.style.display = "none";
        return 0;
    }
}





function selectDate(div,field,id,fmt) {
    var cal = new CalendarPopup(div);
    cal.select(field,id,fmt);
}