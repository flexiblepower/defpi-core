# service/model/EchoInterfaceV001/xsd.py
# -*- coding: utf-8 -*-
# PyXB bindings for NM:91a69c08735984d26ed414886891257267230972
# Generated 2017-10-24 10:43:45.126476 by PyXB version 1.2.4 using Python 2.7.12.final.0
# Namespace urn:books

from __future__ import unicode_literals
import pyxb
import pyxb.binding
import pyxb.binding.saxer
import io
import pyxb.utils.utility
import pyxb.utils.domutils
import sys
import pyxb.utils.six as _six

# Unique identifier for bindings created at the same time
_GenerationUID = pyxb.utils.utility.UniqueIdentifier('urn:uuid:720b31e0-b897-11e7-abed-000c29f8afc5')

# Version of PyXB used to generate the bindings
_PyXBVersion = '1.2.4'
# Generated bindings are not compatible across PyXB versions
if pyxb.__version__ != _PyXBVersion:
    raise pyxb.PyXBVersionError(_PyXBVersion)

# Import bindings for namespaces imported into schema
import pyxb.binding.datatypes

# NOTE: All namespace declarations are reserved within the binding
Namespace = pyxb.namespace.NamespaceForURI('urn:books', create_if_missing=True)
Namespace.configureCategories(['typeBinding', 'elementBinding'])

def CreateFromDocument (xml_text, default_namespace=None, location_base=None):
    """Parse the given XML and use the document element to create a
    Python instance.

    @param xml_text An XML document.  This should be data (Python 2
    str or Python 3 bytes), or a text (Python 2 unicode or Python 3
    str) in the L{pyxb._InputEncoding} encoding.

    @keyword default_namespace The L{pyxb.Namespace} instance to use as the
    default namespace where there is no default namespace in scope.
    If unspecified or C{None}, the namespace of the module containing
    this function will be used.

    @keyword location_base: An object to be recorded as the base of all
    L{pyxb.utils.utility.Location} instances associated with events and
    objects handled by the parser.  You might pass the URI from which
    the document was obtained.
    """

    if pyxb.XMLStyle_saxer != pyxb._XMLStyle:
        dom = pyxb.utils.domutils.StringToDOM(xml_text)
        return CreateFromDOM(dom.documentElement, default_namespace=default_namespace)
    if default_namespace is None:
        default_namespace = Namespace.fallbackNamespace()
    saxer = pyxb.binding.saxer.make_parser(fallback_namespace=default_namespace, location_base=location_base)
    handler = saxer.getContentHandler()
    xmld = xml_text
    if isinstance(xmld, _six.text_type):
        xmld = xmld.encode(pyxb._InputEncoding)
    saxer.parse(io.BytesIO(xmld))
    instance = handler.rootObject()
    return instance

def CreateFromDOM (node, default_namespace=None):
    """Create a Python instance from the given DOM node.
    The node tag must correspond to an element declaration in this module.

    @deprecated: Forcing use of DOM interface is unnecessary; use L{CreateFromDocument}."""
    if default_namespace is None:
        default_namespace = Namespace.fallbackNamespace()
    return pyxb.binding.basis.element.AnyCreateFromDOM(node, default_namespace)


# Complex type {urn:books}BooksForm with content type ELEMENT_ONLY
class BooksForm (pyxb.binding.basis.complexTypeDefinition):
    """Complex type {urn:books}BooksForm with content type ELEMENT_ONLY"""
    _TypeDefinition = None
    _ContentTypeTag = pyxb.binding.basis.complexTypeDefinition._CT_ELEMENT_ONLY
    _Abstract = False
    _ExpandedName = pyxb.namespace.ExpandedName(Namespace, 'BooksForm')
    _XSDLocation = pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 7, 2)
    _ElementMap = {}
    _AttributeMap = {}
    # Base type is pyxb.binding.datatypes.anyType
    
    # Element book uses Python identifier book
    __book = pyxb.binding.content.ElementDeclaration(pyxb.namespace.ExpandedName(None, 'book'), 'book', '__urnbooks_BooksForm_book', True, pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 9, 6), )

    
    book = property(__book.value, __book.set, None, None)

    _ElementMap.update({
        __book.name() : __book
    })
    _AttributeMap.update({
        
    })
Namespace.addCategoryObject('typeBinding', 'BooksForm', BooksForm)


# Complex type {urn:books}BookForm with content type ELEMENT_ONLY
class BookForm (pyxb.binding.basis.complexTypeDefinition):
    """Complex type {urn:books}BookForm with content type ELEMENT_ONLY"""
    _TypeDefinition = None
    _ContentTypeTag = pyxb.binding.basis.complexTypeDefinition._CT_ELEMENT_ONLY
    _Abstract = False
    _ExpandedName = pyxb.namespace.ExpandedName(Namespace, 'BookForm')
    _XSDLocation = pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 16, 2)
    _ElementMap = {}
    _AttributeMap = {}
    # Base type is pyxb.binding.datatypes.anyType
    
    # Element author uses Python identifier author
    __author = pyxb.binding.content.ElementDeclaration(pyxb.namespace.ExpandedName(None, 'author'), 'author', '__urnbooks_BookForm_author', False, pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 18, 6), )

    
    author = property(__author.value, __author.set, None, None)

    
    # Element title uses Python identifier title
    __title = pyxb.binding.content.ElementDeclaration(pyxb.namespace.ExpandedName(None, 'title'), 'title', '__urnbooks_BookForm_title', False, pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 19, 6), )

    
    title = property(__title.value, __title.set, None, None)

    
    # Element genre uses Python identifier genre
    __genre = pyxb.binding.content.ElementDeclaration(pyxb.namespace.ExpandedName(None, 'genre'), 'genre', '__urnbooks_BookForm_genre', False, pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 20, 6), )

    
    genre = property(__genre.value, __genre.set, None, None)

    
    # Element price uses Python identifier price
    __price = pyxb.binding.content.ElementDeclaration(pyxb.namespace.ExpandedName(None, 'price'), 'price', '__urnbooks_BookForm_price', False, pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 21, 6), )

    
    price = property(__price.value, __price.set, None, None)

    
    # Element pub_date uses Python identifier pub_date
    __pub_date = pyxb.binding.content.ElementDeclaration(pyxb.namespace.ExpandedName(None, 'pub_date'), 'pub_date', '__urnbooks_BookForm_pub_date', False, pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 22, 6), )

    
    pub_date = property(__pub_date.value, __pub_date.set, None, None)

    
    # Element review uses Python identifier review
    __review = pyxb.binding.content.ElementDeclaration(pyxb.namespace.ExpandedName(None, 'review'), 'review', '__urnbooks_BookForm_review', False, pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 23, 6), )

    
    review = property(__review.value, __review.set, None, None)

    
    # Attribute id uses Python identifier id
    __id = pyxb.binding.content.AttributeUse(pyxb.namespace.ExpandedName(None, 'id'), 'id', '__urnbooks_BookForm_id', pyxb.binding.datatypes.string)
    __id._DeclarationLocation = pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 25, 4)
    __id._UseLocation = pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 25, 4)
    
    id = property(__id.value, __id.set, None, None)

    _ElementMap.update({
        __author.name() : __author,
        __title.name() : __title,
        __genre.name() : __genre,
        __price.name() : __price,
        __pub_date.name() : __pub_date,
        __review.name() : __review
    })
    _AttributeMap.update({
        __id.name() : __id
    })
Namespace.addCategoryObject('typeBinding', 'BookForm', BookForm)


books = pyxb.binding.basis.element(pyxb.namespace.ExpandedName(Namespace, 'books'), BooksForm, location=pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 5, 2))
Namespace.addCategoryObject('elementBinding', books.name().localName(), books)



BooksForm._AddElement(pyxb.binding.basis.element(pyxb.namespace.ExpandedName(None, 'book'), BookForm, scope=BooksForm, location=pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 9, 6)))

def _BuildAutomaton ():
    # Remove this helper function from the namespace after it is invoked
    global _BuildAutomaton
    del _BuildAutomaton
    import pyxb.utils.fac as fac

    counters = set()
    cc_0 = fac.CounterCondition(min=0, max=None, metadata=pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 9, 6))
    counters.add(cc_0)
    states = []
    final_update = set()
    final_update.add(fac.UpdateInstruction(cc_0, False))
    symbol = pyxb.binding.content.ElementUse(BooksForm._UseForTag(pyxb.namespace.ExpandedName(None, 'book')), pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 9, 6))
    st_0 = fac.State(symbol, is_initial=True, final_update=final_update, is_unordered_catenation=False)
    states.append(st_0)
    transitions = []
    transitions.append(fac.Transition(st_0, [
        fac.UpdateInstruction(cc_0, True) ]))
    st_0._set_transitionSet(transitions)
    return fac.Automaton(states, counters, True, containing_state=None)
BooksForm._Automaton = _BuildAutomaton()




BookForm._AddElement(pyxb.binding.basis.element(pyxb.namespace.ExpandedName(None, 'author'), pyxb.binding.datatypes.string, scope=BookForm, location=pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 18, 6)))

BookForm._AddElement(pyxb.binding.basis.element(pyxb.namespace.ExpandedName(None, 'title'), pyxb.binding.datatypes.string, scope=BookForm, location=pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 19, 6)))

BookForm._AddElement(pyxb.binding.basis.element(pyxb.namespace.ExpandedName(None, 'genre'), pyxb.binding.datatypes.string, scope=BookForm, location=pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 20, 6)))

BookForm._AddElement(pyxb.binding.basis.element(pyxb.namespace.ExpandedName(None, 'price'), pyxb.binding.datatypes.float, scope=BookForm, location=pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 21, 6)))

BookForm._AddElement(pyxb.binding.basis.element(pyxb.namespace.ExpandedName(None, 'pub_date'), pyxb.binding.datatypes.date, scope=BookForm, location=pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 22, 6)))

BookForm._AddElement(pyxb.binding.basis.element(pyxb.namespace.ExpandedName(None, 'review'), pyxb.binding.datatypes.string, scope=BookForm, location=pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 23, 6)))

def _BuildAutomaton_ ():
    # Remove this helper function from the namespace after it is invoked
    global _BuildAutomaton_
    del _BuildAutomaton_
    import pyxb.utils.fac as fac

    counters = set()
    states = []
    final_update = None
    symbol = pyxb.binding.content.ElementUse(BookForm._UseForTag(pyxb.namespace.ExpandedName(None, 'author')), pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 18, 6))
    st_0 = fac.State(symbol, is_initial=True, final_update=final_update, is_unordered_catenation=False)
    states.append(st_0)
    final_update = None
    symbol = pyxb.binding.content.ElementUse(BookForm._UseForTag(pyxb.namespace.ExpandedName(None, 'title')), pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 19, 6))
    st_1 = fac.State(symbol, is_initial=False, final_update=final_update, is_unordered_catenation=False)
    states.append(st_1)
    final_update = None
    symbol = pyxb.binding.content.ElementUse(BookForm._UseForTag(pyxb.namespace.ExpandedName(None, 'genre')), pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 20, 6))
    st_2 = fac.State(symbol, is_initial=False, final_update=final_update, is_unordered_catenation=False)
    states.append(st_2)
    final_update = None
    symbol = pyxb.binding.content.ElementUse(BookForm._UseForTag(pyxb.namespace.ExpandedName(None, 'price')), pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 21, 6))
    st_3 = fac.State(symbol, is_initial=False, final_update=final_update, is_unordered_catenation=False)
    states.append(st_3)
    final_update = None
    symbol = pyxb.binding.content.ElementUse(BookForm._UseForTag(pyxb.namespace.ExpandedName(None, 'pub_date')), pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 22, 6))
    st_4 = fac.State(symbol, is_initial=False, final_update=final_update, is_unordered_catenation=False)
    states.append(st_4)
    final_update = set()
    symbol = pyxb.binding.content.ElementUse(BookForm._UseForTag(pyxb.namespace.ExpandedName(None, 'review')), pyxb.utils.utility.Location('/home/coenvl/Develop/M2M/def-pi/skeleton_python/resources/xsd/EchoInterfaceV001.xsd', 23, 6))
    st_5 = fac.State(symbol, is_initial=False, final_update=final_update, is_unordered_catenation=False)
    states.append(st_5)
    transitions = []
    transitions.append(fac.Transition(st_1, [
         ]))
    st_0._set_transitionSet(transitions)
    transitions = []
    transitions.append(fac.Transition(st_2, [
         ]))
    st_1._set_transitionSet(transitions)
    transitions = []
    transitions.append(fac.Transition(st_3, [
         ]))
    st_2._set_transitionSet(transitions)
    transitions = []
    transitions.append(fac.Transition(st_4, [
         ]))
    st_3._set_transitionSet(transitions)
    transitions = []
    transitions.append(fac.Transition(st_5, [
         ]))
    st_4._set_transitionSet(transitions)
    transitions = []
    st_5._set_transitionSet(transitions)
    return fac.Automaton(states, counters, False, containing_state=None)
BookForm._Automaton = _BuildAutomaton_()

