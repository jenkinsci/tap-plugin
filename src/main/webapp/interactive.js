extendedTapsetInteractives = function () {

  var batchRunning = false;

  /**
  For all elements of given class,
  if they are  hidden (display == none) the display is set to block
  otherwise it is set to none
  **/
  function showHideBlock(clazz) {
    showHide(clazz, 'block')
  }

  /**
  For all elements of given class,
  if they are  hidden (display == none) the display is set to table-row
  otherwise it is set to none
  **/
  function showHideRow(clazz) {
    showHide(clazz, 'table-row')
  }

  /**
  Shortcut method to show/hide prefixed input classes as table-row
  **/
  function showHideRowGroup(clazz) {
    showHideRow("test_" + clazz)
    showHideRow("tr_details_" + clazz)
  }

  /**
  For all elements of given class,
  if they are  hidden (display == none) the display is set to inline
  otherwise it is set to none
  **/
  function showHideInline(clazz, type) {
    showHide(clazz, 'inline')
  }

  /**
  For all elements of given class,
  if they are  hidden (display == none) the display is set to given type
  otherwise it is set to none
  **/
  function showHide(clazz, type) {
    var all = document.getElementsByClassName(clazz);
    for (var i = 0; i < all.length; i++) {
      if (all[i].style != null) if (all[i].style.display == 'none') { all[i].style.display = type } else { all[i].style.display = 'none' };
    }
    fixButtons();
  }

  /**
  For one exact element of given id
  if it is  hidden (display == none) the display is set to table-row
  otherwise it is set to none
  **/
  function showHideRowOne(id) {
    showHideOne(id, 'table-row')
  }

  /**
  For one exact element of given id
  if it is  hidden (display == none) the display is set to inline
  otherwise it is set to none
  **/
  function showHideInlineOne(id) {
    showHideOne(id, 'inline')
  }

  /**
  For one exact element of given id
  if it is  hidden (display == none) the display is set to block
  otherwise it is set to none
  **/
  function showHideBlockOne(id) {
    showHideOne(id, 'block')
  }

  /**
  For one exact element of given id
  if it is  hidden (display == none) the display is set to given type
  otherwise it is set to none
  **/
  function showHideOne(id, type) {
    var el = document.getElementById(id);
    if (el != null && el.style != null) if (el.style.display == 'none') { el.style.display = type } else { el.style.display = 'none' };
    fixButtons();
  }

  /**
  Utility method which set display property of all elements of given class to value of type
  **/
  function set(clazz, type) {
    var all = document.getElementsByClassName(clazz);
    for (var i = 0; i < all.length; i++) {
      if (all[i].style != null) {all[i].style.display = type;}
    }
    fixButtons();
  }
  /**
    Utility method to show/hide ok rows
  **/
  function showHideOk() {
    showHideRowGroup("ok")
  }

  /**
    Utility method to show/hide ok rows
  **/
  function showHideNotOk() {
    showHideRowGroup("not_ok")
  }
  /**
  Utility method tho show/hide various skip declarations
  **/
  function showHideSkips() {
    showHideRowGroup("ok_SKIP")
    showHideRowGroup("not_ok_SKIP")
  }

  /**
  Utility method tho show/hide various todo and details declarations (this is not a todo:)
  **/
  function showHideTodos() {
    showHideRowGroup("ok_TODO")
    showHideRowGroup("not_ok_TODO")
  }

  function showHideComments(){
    showHideRow("_comment_")
  }

  function showHideBails(){
    showHideRow("_bailout_")
  }

  function showHideDetailsRows() {
    showHideRow("tr_details_ok")
    showHideRow("tr_details_not_ok")
  }

  function showHideDetails() {
    showHideBlock("detail_body")
  }
  /**
  Utility method tho set various skip declarations
  **/
  function setSkips(value) {
      set("test_ok_SKIP", value)
      set("test_not_ok_SKIP", value)
  }

  /**
  Utility method tho set various todo declarations
  **/
  function setTodos(value) {
    set("test_ok_TODO", value)
    set("test_not_ok_TODO", value)
  }

  /**
  Utility method tho set various skip declarations in tables
  **/
  function setTrSkips(value) {
      set("tr_details_ok_SKIP", value)
      set("tr_details_not_ok_SKIP", value)
  }

  /**
  Utility method tho set various todo declarations in tables
  **/
  function setTrTodos(value) {
    set("tr_details_ok_TODO", value)
    set("tr_details_not_ok_TODO", value)
  }
  /**
  Shortcut method to set display of all not-ok elements to table-row, and none to others
  **/
  function showFailed() {
    batchRunning = true
    try {
      set("test_ok", "none")
      set("test_not_ok", "table-row")
      set("_bailout_", "table-row")
      setSkips("none")
      setTodos("none")
      set("tr_details_ok", "none")
      set("tr_details_not_ok", "none")
      setTrSkips("none")
      setTrTodos("none")
      set("_comment_", "none")
      set("detail_body", "none")
    } finally {
      batchRunning=false
      fixButtons();
    }
  }

  /**
  Shortcut method to set display of all not-ok elements and theirs details to table-row, and none to others
  **/
  function showFailedDetails() {
  batchRunning = true
    try {
      set("test_ok", "none")
      set("test_not_ok", "table-row")
      set("_bailout_", "table-row")
      setSkips("none")
      setTodos("none")
      set("tr_details_ok", "none")
      set("tr_details_not_ok", "table-row")
      setTrSkips("none")
      setTrTodos("none")
      set("_comment_", "none")
      set("detail_body", "block")
    } finally {
      batchRunning=false
      fixButtons();
    }
  }

  /**
  Shortcut method to set display of all not-ok elements and theirs details to table-row, and none to others.
  ok-skip is set to table-none to hide details body, but keep the its row opened for user-expansion-ondemand
  **/
  function hideDetails() {
    batchRunning = true
    try{
      set("test_ok", "none")
      set("test_not_ok", "table-row")
      set("_bailout_", "table-row")
      setSkips("none")
      setTodos("none")
      set("tr_details_ok", "none")
      set("tr_details_not_ok", "table-row")
      setTrSkips("table-none")
      setTrTodos("table-none")
      set("_comment_", "none")
      set("detail_body", "none")
    } finally {
      batchRunning=false
      fixButtons();
    }
  }

  /**
  Shortcut method to set display of all elements to none
  **/
  function showNothing() {
    batchRunning = true
    try{
      set("test_ok", "none")
      set("test_not_ok", "none")
      set("_bailout_", "none")
      setSkips("none")
      setTodos("none")
      set("tr_details_ok", "none")
      set("tr_details_not_ok", "none")
      setTrSkips("none")
      setTrTodos("none")
      set("_comment_", "none")
      set("detail_body", "none")
    } finally {
      batchRunning=false
      fixButtons();
    }
  }

  /**
  Shortcut method to set display of all elements to block or table-row
  **/
  function showAll() {
    batchRunning = true
    try {
      set("test_ok", "table-row")
      set("test_not_ok", "table-row")
      set("_bailout_", "table-row")
      setSkips("table-row")
      setTodos("table-row")
      set("tr_details_ok", "table-row")
      set("tr_details_not_ok", "table-row")
      setTrSkips("table-row")
      setTrTodos("table-row")
      set("_comment_", "table-row")
      set("detail_body", "block")
    } finally {
      batchRunning=false
      fixButtons();
    }
  }

  //you can not pass ..args to another ..args
  //otherwise one would end in array of arrays
  //so calling getMultipleClasses directly, requires array on input
  function getMultipleClasses(args) {
      var all = new Array();
      for(let arg of args) {
        var singleClass = Array.from(document.getElementsByClassName(arg))
        all = all.concat(singleClass)
      }
      var set = new Set(all);
      const result = [];
      set.forEach(v => result.push(v)); //Array.from(set) was seen to not work always
      return result;
    }

    //-1 all none - all invisible
    //+1 none none - all visible
    //0 mixed - some visible, some not (happens mostly for details)
    function getSelectState(clazz) {
      var all = getMultipleClasses(clazz);
      if (all.length == 0) {
        return 0;
      }
      var none = 0;
      var something = 0;
      for (var i = 0; i < all.length; i++) {
        if (all[i].style == null || all[i].style.display == 'none') { none++ } else { something++ };
      }
      if (none == all.length) {
        return -1;
      } else if (something == all.length) {
        return 1;
      } else {
        return 0;
      }
    }

    function setButtonByState(button, state) {
      if (state == -1) {
        button.classList.remove("tapseExtendedMixed")
        button.classList.remove("tapseExtendedReleased")
        button.classList.add("tapseExtendedPushed")
      } else if (state == 1) {
        button.classList.remove("tapseExtendedMixed")
        button.classList.remove("tapseExtendedPushed")
        button.classList.add("tapseExtendedReleased")
      } else {
        button.classList.remove("tapseExtendedPushed")
        button.classList.remove("tapseExtendedReleased")
        button.classList.add("tapseExtendedMixed")
      }
    }

    function fixButtons() {
      if (batchRunning) {
        return;
      }
      var btn = document.getElementById("okTestsButton");
      var state = getSelectState(["test_ok"]);
      setButtonByState(btn, state);
      var btn = document.getElementById("notOkTestsButton");
      var state = getSelectState(["test_not_ok"]);
      setButtonByState(btn, state);
      var btn = document.getElementById("commentsTestsButton");
      if (btn!=null) { //comments can be disabeld in settings
        var state = getSelectState(["_comment_"]);
        setButtonByState(btn, state);
      }
      var btn = document.getElementById("bailedTestsButton");
      var state = getSelectState(["_bailout_"]);
      setButtonByState(btn, state);
      var btn = document.getElementById("detailsTestsButton");
      var state = getSelectState(["detail_body"]);
      setButtonByState(btn, state);
      var btn = document.getElementById("skippedTestsButton");
      var state = getSelectState(["test_ok_SKIP", "tr_details_ok_SKIP", "test_not_ok_SKIP", "tr_details_not_ok_SKIP"]);
      setButtonByState(btn, state);
      var btn = document.getElementById("todoTestsButton");
      var state = getSelectState(["test_ok_TODO", "tr_details_ok_TODO", "test_not_ok_TODO", "tr_details_not_ok_TODO"]);
      setButtonByState(btn, state);
      var btn = document.getElementById("detailRowsTestsButton");
      var state = getSelectState(["tr_details_ok", "tr_details_not_ok"]);
      setButtonByState(btn, state);

      var btn = document.getElementById("plusMinusTestsButton");
      var state = getSelectState(["jsPM"]);
      setButtonByState(btn, state);
    }


  return {
    showHideBlock: showHideBlock,
    showHideRow: showHideRow,
    showHideOk: showHideOk,
    showHideNotOk: showHideNotOk,
    showHideInline: showHideInline,
    //showHide: showHideRowGroup,showHide,  //no need to publish
    showHideRowOne: showHideRowOne,
    showHideInlineOne: showHideInlineOne,
    showHideBlockOne: showHideBlockOne,
    showHideOne: showHideOne,
    //set: set,           //no need to publish
    showFailed: showFailed,
    showFailedDetails: showFailedDetails,
    hideDetails: hideDetails,
    showNothing: showNothing,
    showAll: showAll,
    //setSkips,setTodos,setTrSkips,setTrTodos //no need to publish
    showHideSkips: showHideSkips,
    showHideTodos: showHideTodos,
    showHideComments: showHideComments,
    showHideBails: showHideBails,
    showHideDetailsRows: showHideDetailsRows,
    showHideDetails: showHideDetails,
    fixButtons:fixButtons
  }

}
