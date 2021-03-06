package org.hy.common.report;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.POIXMLProperties.CoreProperties;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFPictureData;
import org.apache.poi.hssf.usermodel.HSSFPrintSetup;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HeaderFooter;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFDrawing;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFPrintSetup;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hy.common.Date;
import org.hy.common.ExpireMap;
import org.hy.common.Help;
import org.hy.common.PartitionMap;
import org.hy.common.TablePartition;
import org.hy.common.report.bean.CacheSheetInfo;
import org.hy.common.report.bean.ImageAreaInfo;
import org.hy.common.report.bean.RCell;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker;
import org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.CTProperties;





/**
 * Excel操作的辅助类 
 *
 * @author      ZhengWei(HY)
 * @createDate  2017-03-16
 * @version     v1.0
 *              v2.0  2017-06-21  优化：合并单元格的区域信息添加缓存中，不用每次都生成一次
 *                                优化：合并单元格的图像信息添加缓存中，不用每次都生成一次
 *                                优化：通过isSafe参数控制，放弃一些非必要的效验来提高性能
 *                                优化：启用对SXSSFWorkbook工作薄的支持大数据量
 *              v3.0  2017-06-22  添加：文档摘要的复制功能
 */
public class ExcelHelp
{
    
    /** 缓存保存的时长（单位秒） */
    public  static int $CacheTimeLen = 60;
    
    private static ExpireMap<CacheSheetInfo ,List<CellRangeAddress>> $MergedRegionsAreaCaches = new ExpireMap<CacheSheetInfo ,List<CellRangeAddress>>();
    
    private static ExpireMap<CacheSheetInfo ,List<ImageAreaInfo>>    $ImageAreaCaches         = new ExpireMap<CacheSheetInfo ,List<ImageAreaInfo>>();
    
    
    
    /**
     * 私有构建器
     */
    protected ExcelHelp()
    {
        
    }
    
    
    
    /**
     * 读取Excel中所有的工作表对象
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-16
     * @version     v1.0
     *
     * @param i_ExcelFileName  文件全路径+文件名称
     * @return
     */
    public final static List<Sheet> read(String i_ExcelFileName)
    {
        List<Sheet> v_Ret   = new ArrayList<Sheet>();
        InputStream     v_Input = null;
        
        try
        {
            if ( i_ExcelFileName.startsWith("file:") )
            {
                URL v_URL = new URL(i_ExcelFileName);
                v_Input = new FileInputStream(v_URL.getFile());
            }
            else
            {
                v_Input = new FileInputStream(i_ExcelFileName);
            }
            
            Workbook v_Workbook   = WorkbookFactory.create(v_Input);
            int      v_SheetCount = v_Workbook.getNumberOfSheets();
            
            for (int v_Index=0; v_Index<v_SheetCount; v_Index++)
            {
                v_Ret.add(v_Workbook.getSheetAt(v_Index));
            }
        }
        catch (Exception exce)
        {
            exce.printStackTrace();
        }
        finally
        {
            if ( null != v_Input )
            {
                try
                {
                    v_Input.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
            }
        }
        
        return v_Ret;
    }
    
    
    
    public final static PartitionMap<String ,RCell> readDatas(String i_ExcelFileName ,int i_SheetIndex)
    {
        return readDatas(i_ExcelFileName ,i_SheetIndex ,null ,null);
    }
    
    
    
    public final static PartitionMap<String ,RCell> readDatas(String i_ExcelFileName ,int i_SheetIndex ,Integer i_BeginRow)
    {
        return readDatas(i_ExcelFileName ,i_SheetIndex ,i_BeginRow ,null);
    }
    
    
    
    public final static PartitionMap<String ,RCell> readDatas(String i_ExcelFileName ,int i_SheetIndex ,Integer i_BeginRow ,Integer i_EndRow)
    {
        return readDatas(read(i_ExcelFileName).get(i_SheetIndex) ,i_BeginRow ,i_EndRow);
    }
    
    
    
    public final static PartitionMap<String ,RCell> readDatas(Sheet i_Sheet)
    {
        return readDatas(i_Sheet ,null ,null);
    }
    
    
    
    public final static PartitionMap<String ,RCell> readDatas(Sheet i_Sheet ,Integer i_BeginRow)
    {
        return readDatas(i_Sheet ,i_BeginRow ,null);
    }
    
    
    
    public final static PartitionMap<String ,RCell> readDatas(Sheet i_Sheet ,Integer i_BeginRow ,Integer i_EndRow)
    {
        PartitionMap<String ,RCell> v_Ret      = new TablePartition<String ,RCell>();
        Sheet                       v_Sheet    = i_Sheet;
        int                         v_BeginRow = 0;
        int                         v_EndRow   = 0;
        
        if ( i_BeginRow != null )
        {
            v_BeginRow = i_BeginRow.intValue();
            
            if ( v_BeginRow < 0 )
            {
                v_BeginRow = 0;
            }
        }
        
        if ( i_EndRow != null )
        {
            v_EndRow = i_EndRow.intValue();
        }
        else
        {
            v_EndRow = v_Sheet.getPhysicalNumberOfRows();
        }
        
        for (int v_RowNo=v_BeginRow; v_RowNo<=v_EndRow; v_RowNo++)
        {
            Row v_Row = v_Sheet.getRow(v_RowNo);
            if ( v_Row == null )
            {
                continue;
            }
            
            short v_CellCount = v_Row.getLastCellNum();
            
            for (int v_ColumnNo=0; v_ColumnNo<v_CellCount; v_ColumnNo++)
            {
                Cell v_Cell = v_Row.getCell(v_ColumnNo);
                if ( v_Cell == null )
                {
                    continue;
                }
                
                if ( v_Cell.getCellTypeEnum() == CellType.STRING )
                {
                    String v_Value = v_Cell.getStringCellValue();
                    
                    if ( !Help.isNull(v_Value) )
                    {
                        v_Ret.putRow(v_Value.trim() ,new RCell(v_RowNo ,v_ColumnNo));
                    }
                }
                else if ( v_Cell.getCellTypeEnum() == CellType.NUMERIC )
                {
                    if ( HSSFDateUtil.isCellDateFormatted(v_Cell) ) 
                    {
                        if ( v_Cell.getDateCellValue() != null )
                        {
                            v_Ret.putRow((new Date(v_Cell.getDateCellValue())).getFull() ,new RCell(v_RowNo ,v_ColumnNo));
                        }
                    } 
                    else 
                    {
                        v_Ret.putRow(String.valueOf(v_Cell.getNumericCellValue()) ,new RCell(v_RowNo ,v_ColumnNo));
                    }
                }
            }
        }
        
        return v_Ret;
    }
    
    
    
    /**
     * 保存工作薄
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-17
     * @version     v1.0
     *
     * @param i_Workbook  工作薄对象
     * @param i_SaveFile  保存的全路径+文件名称。当没有写扩展名称或类型不匹配时，自动识别添加
     * @return            保存成功返回：文件全路径+文件名称；异常返回：null
     */
    public final static String save(Workbook i_Workbook ,String i_SaveFile)
    {
        String v_SaveFile = i_SaveFile.trim();
        if ( i_Workbook instanceof HSSFWorkbook )
        {
            if ( !v_SaveFile.toLowerCase().endsWith(".xls") )
            {
                v_SaveFile += ".xls";
            }
        }
        else if ( i_Workbook instanceof SXSSFWorkbook )
        {
            if ( !v_SaveFile.toLowerCase().endsWith(".xlsx") )
            {
                v_SaveFile += ".xlsx";
            }
        }
        else if ( i_Workbook instanceof XSSFWorkbook )
        {
            if ( !v_SaveFile.toLowerCase().endsWith(".xlsx") )
            {
                v_SaveFile += ".xlsx";
            }
        }
        
        FileOutputStream v_Output   = null;
        
        try
        {            
            
            v_Output = new FileOutputStream(v_SaveFile);
            i_Workbook.write(v_Output);
        }
        catch (Exception exce)
        {
            v_SaveFile = null;
            exce.printStackTrace();
        }
        finally
        {
            if ( v_Output != null )
            {
                try
                {
                    v_Output.flush();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
                
                try
                {
                    v_Output.close();
                }
                catch (Exception exce)
                {
                    // Nothing.
                }
                
                v_Output = null;
            }
        }
        
        return v_SaveFile;
    }
    
    
    
    /**
     * 创建一个工作表
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-16
     * @version     v1.0
     *
     * @param i_Workbook   工作薄对象
     * @param i_SheetName  工作表名称(当为空时，自动生成)
     * @return
     */
    public final static Sheet createSheet(Workbook i_Workbook ,String i_SheetName) 
    {
        int    v_SheetCount = i_Workbook.getNumberOfSheets();
        String v_SheetName  = i_SheetName;
        
        if ( Help.isNull(v_SheetName) ) 
        {
            v_SheetName = "sheet" + (v_SheetCount + 1);
        }
        
        return i_Workbook.createSheet(v_SheetName);
    }
    
    
    
    /**
     * 复制工作薄相关参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-06-22
     * @version     v1.0
     *
     * @param i_FromWB
     * @param i_ToWB
     */
    public final static void copyWorkbook(Workbook i_FromWB ,Workbook i_ToWB)
    {
        if ( i_ToWB instanceof HSSFWorkbook )
        {
            DocumentSummaryInformation v_FromSummary = ((HSSFWorkbook)i_FromWB).getDocumentSummaryInformation();
            
            if ( v_FromSummary == null )
            {
                return;
            }
            
            ((HSSFWorkbook)i_ToWB).createInformationProperties();
            DocumentSummaryInformation v_ToSummary = ((HSSFWorkbook)i_ToWB).getDocumentSummaryInformation();
            
            v_ToSummary.setCategory(v_FromSummary.getCategory());  // 类别
            v_ToSummary.setManager( v_FromSummary.getManager());   // 管理者
            v_ToSummary.setCompany( v_FromSummary.getCompany());   // 公司
            
            SummaryInformation v_FromSummary2 = ((HSSFWorkbook)i_FromWB).getSummaryInformation();
            SummaryInformation v_ToSummary2   = ((HSSFWorkbook)i_ToWB  ).getSummaryInformation();
            
            v_ToSummary2.setSubject( v_FromSummary2.getSubject());  // 主题
            v_ToSummary2.setTitle(   v_FromSummary2.getTitle());    // 标题
            v_ToSummary2.setAuthor(  v_FromSummary2.getAuthor());   // 作者
            v_ToSummary2.setComments(v_FromSummary2.getComments()); // 备注
        }
        else if ( i_ToWB instanceof SXSSFWorkbook 
               || i_ToWB instanceof XSSFWorkbook )
        {
            XSSFWorkbook v_FromWB = null;
            XSSFWorkbook v_ToWB   = null;
            
            if ( i_FromWB instanceof SXSSFWorkbook )
            {
                v_FromWB = ((SXSSFWorkbook)i_FromWB).getXSSFWorkbook();
            }
            else if ( i_FromWB instanceof XSSFWorkbook )
            {
                v_FromWB = (XSSFWorkbook)i_FromWB;
            }
            else
            {
                return;
            }
            
            if ( i_ToWB instanceof SXSSFWorkbook )
            {
                v_ToWB = ((SXSSFWorkbook)i_ToWB).getXSSFWorkbook();
            }
            else if ( i_ToWB instanceof XSSFWorkbook )
            {
                v_ToWB = (XSSFWorkbook)i_ToWB;
            }
            else
            {
                return;
            }
            
            CoreProperties v_FromCP = v_FromWB.getProperties().getCoreProperties();
            CoreProperties v_ToCP   = v_ToWB  .getProperties().getCoreProperties();
            
            v_ToCP.setCategory(          v_FromCP.getCategory());
            v_ToCP.setCreator(           v_FromCP.getCreator());
            v_ToCP.setDescription(       v_FromCP.getDescription());
            v_ToCP.setIdentifier(        v_FromCP.getIdentifier());
            v_ToCP.setKeywords(          v_FromCP.getKeywords());
            v_ToCP.setLastModifiedByUser(v_FromCP.getLastModifiedByUser());
            v_ToCP.setRevision(          v_FromCP.getRevision());
            v_ToCP.setSubjectProperty(   v_FromCP.getSubject());
            v_ToCP.setTitle(             v_FromCP.getTitle());
            
            CTProperties v_FromCTP = v_FromWB.getProperties().getExtendedProperties().getUnderlyingProperties();
            CTProperties v_ToCTP   = v_ToWB  .getProperties().getExtendedProperties().getUnderlyingProperties();
            
            v_ToCTP.setCompany(      v_FromCTP.getCompany());
            v_ToCTP.setHyperlinkBase(v_FromCTP.getHyperlinkBase());
            v_ToCTP.setManager(      v_FromCTP.getManager());
            v_ToCTP.setTemplate(     v_FromCTP.getTemplate());
        }
    }
    
    
    
    /**
     * 复制模板工作表的整体(所有)列的列宽到数据工作表中
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-17
     * @version     v1.0
     *
     * @param i_FromSheet  源工作表
     * @param i_ToSheet    目标工作表
     */
    public final static void copyColumnsWidth(Sheet i_FromSheet ,Sheet i_ToSheet) 
    {
        Row v_Row = i_FromSheet.getRow(0);
        if ( null == v_Row ) 
        {
            return;
        }
        
        i_ToSheet.setDefaultColumnWidth(i_FromSheet.getDefaultColumnWidth());
        
        short v_ColumnCount = v_Row.getLastCellNum();
        
        if ( i_ToSheet instanceof HSSFSheet )
        {
            HSSFSheet v_ToSheet = (HSSFSheet)i_ToSheet;
            
            for (int v_ColumnIndex = 0; v_ColumnIndex < v_ColumnCount; v_ColumnIndex++) 
            {
                int v_Width = i_FromSheet.getColumnWidth(v_ColumnIndex);
                v_ToSheet.setColumnWidth(v_ColumnIndex ,v_Width);
            }
        }
        else if ( i_ToSheet instanceof SXSSFSheet )
        {
            SXSSFSheet v_ToSheet = (SXSSFSheet)i_ToSheet;
            
            for (int v_ColumnIndex = 0; v_ColumnIndex < v_ColumnCount; v_ColumnIndex++) 
            {
                int v_Width = i_FromSheet.getColumnWidth(v_ColumnIndex);
                v_ToSheet.setColumnWidth(v_ColumnIndex ,v_Width);
            }
        }
        else if ( i_ToSheet instanceof XSSFSheet )
        {
            XSSFSheet v_ToSheet = (XSSFSheet)i_ToSheet;
            
            for (int v_ColumnIndex = 0; v_ColumnIndex < v_ColumnCount; v_ColumnIndex++) 
            {
                int v_Width = i_FromSheet.getColumnWidth(v_ColumnIndex);
                v_ToSheet.setColumnWidth(v_ColumnIndex ,v_Width);
            }
        }
    }
    
    
    
    /**
     * 复制工作表相关参数
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-20
     * @version     v1.0
     *
     * @param i_FromSheet
     * @param i_ToSheet
     */
    public final static void copySheet(Sheet i_FromSheet ,Sheet i_ToSheet)
    {
        // 打印时显示网格线
        i_ToSheet.setPrintGridlines(           i_FromSheet.isPrintGridlines());
        i_ToSheet.setPrintRowAndColumnHeadings(i_FromSheet.isPrintRowAndColumnHeadings());
        i_ToSheet.setFitToPage(                i_FromSheet.getFitToPage());
        
        // Sheet页自适应页面大小
        i_ToSheet.setAutobreaks(               i_FromSheet.getAutobreaks());
        i_ToSheet.setDisplayZeros(             i_FromSheet.isDisplayZeros());
        i_ToSheet.setDisplayGuts(              i_FromSheet.getDisplayGuts());
        // 网格线
        i_ToSheet.setDisplayGridlines(         i_FromSheet.isDisplayGridlines());
        
        // 冻结线
        if ( i_FromSheet.getPaneInformation() != null )
        {
            i_ToSheet.createFreezePane(i_FromSheet.getPaneInformation().getVerticalSplitPosition()
                                      ,i_FromSheet.getPaneInformation().getHorizontalSplitPosition()
                                      ,i_FromSheet.getPaneInformation().getVerticalSplitLeftColumn()
                                      ,i_FromSheet.getPaneInformation().getHorizontalSplitTopRow());
        }
    }
    
    
    
    /**
     * 复制模板工作表的打印区域到数据工作表中
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-17
     * @version     v1.0
     * 
     * @param i_FromSheet  源工作表
     * @param i_ToSheet    目标工作表
     */
    public final static void copyPrintSetup(Sheet i_FromSheet ,Sheet i_ToSheet) 
    {
        PrintSetup v_FromPrintSetup = i_FromSheet.getPrintSetup();
        PrintSetup v_ToPrintSetup   = i_ToSheet  .getPrintSetup();
        
        v_ToPrintSetup.setCopies(       v_FromPrintSetup.getCopies());
        v_ToPrintSetup.setDraft(        v_FromPrintSetup.getDraft());          // 值为true时，表示用草稿品质打印
        v_ToPrintSetup.setFitHeight(    v_FromPrintSetup.getFitHeight());      // 设置页高
        v_ToPrintSetup.setFitWidth(     v_FromPrintSetup.getFitWidth());       // 设置页宽
        v_ToPrintSetup.setFooterMargin( v_FromPrintSetup.getFooterMargin());
        v_ToPrintSetup.setHeaderMargin( v_FromPrintSetup.getHeaderMargin());
        v_ToPrintSetup.setHResolution(  v_FromPrintSetup.getHResolution());
        v_ToPrintSetup.setLandscape(    v_FromPrintSetup.getLandscape());      // true，则表示页面方向为横向；否则为纵向
        v_ToPrintSetup.setLeftToRight(  v_FromPrintSetup.getLeftToRight());    // true表示“先行后列”；false表示“先列后行”
        v_ToPrintSetup.setNoColor(      v_FromPrintSetup.getNoColor());        // 值为true时，表示单色打印
        v_ToPrintSetup.setNoOrientation(v_FromPrintSetup.getNoOrientation()); 
        v_ToPrintSetup.setNotes(        v_FromPrintSetup.getNotes());          // 设置打印批注
        v_ToPrintSetup.setPageStart(    v_FromPrintSetup.getPageStart());      // 设置打印起始页码
        v_ToPrintSetup.setPaperSize(    v_FromPrintSetup.getPaperSize());      // 纸张类型 A4纸 HSSFPrintSetup.A4_PAPERSIZE
        v_ToPrintSetup.setScale(        v_FromPrintSetup.getScale());          // 缩放比例80%(设置为0-100之间的值)
        v_ToPrintSetup.setUsePage(      v_FromPrintSetup.getUsePage());        // 设置打印起始页码是否使用"自动"
        v_ToPrintSetup.setValidSettings(v_FromPrintSetup.getValidSettings());
        v_ToPrintSetup.setVResolution(  v_FromPrintSetup.getVResolution());
        
        // 设置打印参数
        if ( i_ToSheet instanceof HSSFSheet )
        {
            ((HSSFPrintSetup)v_ToPrintSetup).setOptions(((HSSFPrintSetup)v_FromPrintSetup).getOptions());
            
            i_ToSheet.setMargin(HSSFSheet.TopMargin     ,i_FromSheet.getMargin(HSSFSheet.TopMargin));     // 页边距（上）
            i_ToSheet.setMargin(HSSFSheet.BottomMargin  ,i_FromSheet.getMargin(HSSFSheet.BottomMargin));  // 页边距（下）
            i_ToSheet.setMargin(HSSFSheet.LeftMargin    ,i_FromSheet.getMargin(HSSFSheet.LeftMargin));    // 页边距（左）
            i_ToSheet.setMargin(HSSFSheet.RightMargin   ,i_FromSheet.getMargin(HSSFSheet.RightMargin));   // 页边距（右）
            i_ToSheet.setMargin(HSSFSheet.HeaderMargin  ,i_FromSheet.getMargin(HSSFSheet.HeaderMargin));  // 页眉
            i_ToSheet.setMargin(HSSFSheet.FooterMargin  ,i_FromSheet.getMargin(HSSFSheet.FooterMargin));  // 页脚
        }
        else if ( i_ToSheet instanceof SXSSFSheet )
        {
            ((XSSFPrintSetup)v_ToPrintSetup).setOrientation(((XSSFPrintSetup)v_FromPrintSetup).getOrientation());  // 设置方向 
            
            i_ToSheet.setMargin(SXSSFSheet.TopMargin    ,i_FromSheet.getMargin(SXSSFSheet.TopMargin));     // 页边距（上）
            i_ToSheet.setMargin(SXSSFSheet.BottomMargin ,i_FromSheet.getMargin(SXSSFSheet.BottomMargin));  // 页边距（下）
            i_ToSheet.setMargin(SXSSFSheet.LeftMargin   ,i_FromSheet.getMargin(SXSSFSheet.LeftMargin));    // 页边距（左）
            i_ToSheet.setMargin(SXSSFSheet.RightMargin  ,i_FromSheet.getMargin(SXSSFSheet.RightMargin));   // 页边距（右）
            i_ToSheet.setMargin(SXSSFSheet.HeaderMargin ,i_FromSheet.getMargin(SXSSFSheet.HeaderMargin));  // 页眉
            i_ToSheet.setMargin(SXSSFSheet.FooterMargin ,i_FromSheet.getMargin(SXSSFSheet.FooterMargin));  // 页脚
        }
        else if ( i_ToSheet instanceof XSSFSheet )
        {
            ((XSSFPrintSetup)v_ToPrintSetup).setOrientation(((XSSFPrintSetup)v_FromPrintSetup).getOrientation());  // 设置方向 
            
            i_ToSheet.setMargin(XSSFSheet.TopMargin     ,i_FromSheet.getMargin(XSSFSheet.TopMargin));     // 页边距（上）
            i_ToSheet.setMargin(XSSFSheet.BottomMargin  ,i_FromSheet.getMargin(XSSFSheet.BottomMargin));  // 页边距（下）
            i_ToSheet.setMargin(XSSFSheet.LeftMargin    ,i_FromSheet.getMargin(XSSFSheet.LeftMargin));    // 页边距（左）
            i_ToSheet.setMargin(XSSFSheet.RightMargin   ,i_FromSheet.getMargin(XSSFSheet.RightMargin));   // 页边距（右）
            i_ToSheet.setMargin(XSSFSheet.HeaderMargin  ,i_FromSheet.getMargin(XSSFSheet.HeaderMargin));  // 页眉
            i_ToSheet.setMargin(XSSFSheet.FooterMargin  ,i_FromSheet.getMargin(XSSFSheet.FooterMargin));  // 页脚
        }
        
        copyHeaderFooter(i_FromSheet.getHeader() ,i_ToSheet.getHeader());
        copyHeaderFooter(i_FromSheet.getFooter() ,i_ToSheet.getFooter());
    }
    
    
    
    /**
     * 复制页眉、页脚的文字信息
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-06-22
     * @version     v1.0
     *
     * @param i_FromHF
     * @param i_ToHF
     */
    public final static void copyHeaderFooter(HeaderFooter i_FromHF ,HeaderFooter i_ToHF)
    {
        i_ToHF.setLeft(  i_FromHF.getLeft());
        i_ToHF.setCenter(i_FromHF.getCenter());
        i_ToHF.setRight( i_FromHF.getRight());
    }
    
    
    
    /**
     * 复制模板工作表的合并单元格到数据工作表中
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-17
     * @version     v1.0
     *              v2.0  2017-06-21  添加：合并单元格的区域信息添加缓存中，不用每次都生成一次
     * 
     * @param i_FromSheet     模板工作表
     * @param i_AreaBeginRow  定指区域内的开始行号。包含此行。
     * @param i_AreaEndRow    定指区域内的结束行号。包含此行。
     * @param i_ToSheet       数据工作表
     * @param i_OffsetRow     偏移行号
     * @param i_IsSafe        是要安全？还是要性能
     */
    public final static void copyMergedRegions(Sheet i_FromSheet ,int i_AreaBeginRow ,int i_AreaEndRow ,Sheet i_ToSheet ,int i_OffsetRow ,boolean i_IsSafe) 
    {
        int v_MergedRegionsCount = i_FromSheet.getNumMergedRegions();
        CacheSheetInfo         v_CacheKey   = new CacheSheetInfo(i_FromSheet ,i_AreaBeginRow ,i_AreaEndRow);
        List<CellRangeAddress> v_CacheDatas = $MergedRegionsAreaCaches.get(v_CacheKey);
        
        if ( v_CacheDatas == null )
        {
            v_CacheDatas = new ArrayList<CellRangeAddress>();
            
            for (int i=0; i<v_MergedRegionsCount; i++) 
            {
                CellRangeAddress v_CellRangeAddress = i_FromSheet.getMergedRegion(i);
                
                if ( i_AreaBeginRow <= v_CellRangeAddress.getFirstRow() 
                  && i_AreaEndRow   >= v_CellRangeAddress.getLastRow() )
                {
                    // Nothing. 在区域内的
                }
                else
                {
                    continue;
                }
                
                v_CacheDatas.add(v_CellRangeAddress);
            }
            
            $MergedRegionsAreaCaches.put(v_CacheKey ,v_CacheDatas ,$CacheTimeLen);
        }
        
        if ( Help.isNull(v_CacheDatas) ) return;
        
        // 为了执行性能而分成两个For
        if ( i_IsSafe )
        {
            for (CellRangeAddress v_CellRA : v_CacheDatas) 
            {
                int v_FirstRow = v_CellRA.getFirstRow();
                int v_LastRow  = v_CellRA.getLastRow();
                
                v_FirstRow += i_OffsetRow;
                v_LastRow  += i_OffsetRow;
                
                addMergedRegionsSafe(i_ToSheet 
                                    ,v_FirstRow 
                                    ,v_LastRow 
                                    ,v_CellRA.getFirstColumn() 
                                    ,v_CellRA.getLastColumn());
            }
        }
        else
        {
            for (CellRangeAddress v_CellRA : v_CacheDatas) 
            {
                int v_FirstRow = v_CellRA.getFirstRow();
                int v_LastRow  = v_CellRA.getLastRow();
                
                v_FirstRow += i_OffsetRow;
                v_LastRow  += i_OffsetRow;
                
                addMergedRegionsUnsafe(i_ToSheet 
                                      ,v_FirstRow 
                                      ,v_LastRow 
                                      ,v_CellRA.getFirstColumn() 
                                      ,v_CellRA.getLastColumn());
            }
        }
        
    }
    
    
    
    /**
     * 合并单元格
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-06-21
     * @version     v1.0
     *
     * @param i_Sheet        工作表
     * @param i_FirstRow     首行
     * @param i_LastRow      尾行
     * @param i_FirstColumn  首列
     * @param i_LastColumn   尾列
     * @param i_IsSafe       是要安全？还是要性能
     */
    public final static void addMergedRegions(Sheet i_Sheet ,int i_FirstRow ,int i_LastRow ,int i_FirstColumn ,int i_LastColumn ,boolean i_IsSafe)
    {
        CellRangeAddress v_CellRA = new CellRangeAddress(i_FirstRow 
                                                        ,i_LastRow 
                                                        ,i_FirstColumn
                                                        ,i_LastColumn);
        
        if ( i_IsSafe )
        {
            i_Sheet.addMergedRegion(v_CellRA);
        }
        else
        {
            i_Sheet.addMergedRegionUnsafe(v_CellRA);
        }
    }
    
    
    
    /**
     * 合并单元格
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-06-21
     * @version     v1.0
     *
     * @param i_Sheet        工作表
     * @param i_FirstRow     首行
     * @param i_LastRow      尾行
     * @param i_FirstColumn  首列
     * @param i_LastColumn   尾列
     * @param i_IsSafe       是要安全？还是要性能
     */
    public final static void addMergedRegionsSafe(Sheet i_Sheet ,int i_FirstRow ,int i_LastRow ,int i_FirstColumn ,int i_LastColumn)
    {
        CellRangeAddress v_CellRA = new CellRangeAddress(i_FirstRow 
                                                        ,i_LastRow 
                                                        ,i_FirstColumn
                                                        ,i_LastColumn);
        
        i_Sheet.addMergedRegion(v_CellRA);
    }
    
    
    
    /**
     * 合并单元格
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-06-21
     * @version     v1.0
     *
     * @param i_Sheet        工作表
     * @param i_FirstRow     首行
     * @param i_LastRow      尾行
     * @param i_FirstColumn  首列
     * @param i_LastColumn   尾列
     * @param i_IsSafe       是要安全？还是要性能
     */
    public final static void addMergedRegionsUnsafe(Sheet i_Sheet ,int i_FirstRow ,int i_LastRow ,int i_FirstColumn ,int i_LastColumn)
    {
        CellRangeAddress v_CellRA = new CellRangeAddress(i_FirstRow 
                                                        ,i_LastRow 
                                                        ,i_FirstColumn
                                                        ,i_LastColumn);
        
        i_Sheet.addMergedRegionUnsafe(v_CellRA);
    }
    
    
    
    /**
     * 复制模板工作表的定指区域内的所有图片到数据工作表中
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-17
     * @version     v1.0
     *              v2.0  2017-06-21  添加：合并单元格的图像信息添加缓存中，不用每次都生成一次
     *
     * @param i_FromSheet     模板工作表
     * @param i_AreaBeginRow  定指区域内的开始行号。包含此行。
     * @param i_AreaEndRow    定指区域内的结束行号。包含此行。
     * @param i_ToSheet       数据工作表
     * @param i_OffsetRow     偏移行号
     */
    public final static void copyImages(Sheet i_FromSheet ,int i_AreaBeginRow ,int i_AreaEndRow ,Sheet i_ToSheet, int i_OffsetRow)
    {
        CacheSheetInfo      v_CacheKey   = new CacheSheetInfo(i_FromSheet ,i_AreaBeginRow ,i_AreaEndRow);
        List<ImageAreaInfo> v_CacheDatas = $ImageAreaCaches.get(v_CacheKey);
        
        if ( i_ToSheet instanceof HSSFSheet )
        {
            if ( v_CacheDatas == null )
            {
                v_CacheDatas = new ArrayList<ImageAreaInfo>();
                
                HSSFSheet             v_FromSheet = (HSSFSheet) i_FromSheet;
                List<HSSFPictureData> v_Pictures  = v_FromSheet.getWorkbook().getAllPictures();
                
                if ( i_FromSheet.getDrawingPatriarch() != null ) 
                {
                    for (HSSFShape v_Shape : v_FromSheet.getDrawingPatriarch().getChildren()) 
                    {
                        if ( v_Shape instanceof HSSFPicture) 
                        {
                            HSSFPicture      v_Picture       = (HSSFPicture) v_Shape;
                            HSSFClientAnchor v_Anchor        = v_Picture.getClientAnchor();
                            HSSFPictureData  v_PictureData   = v_Pictures.get(v_Picture.getPictureIndex() - 1);
                            
                            if ( i_AreaBeginRow <= v_Anchor.getRow1() 
                              && i_AreaEndRow   >= v_Anchor.getRow2() )
                            {
                                // Nothing. 在数据区域内的图片
                            }
                            else
                            {
                                continue;
                            }
                            
                            v_CacheDatas.add(new ImageAreaInfo(v_Anchor ,v_PictureData));
                        }
                    }
                }
                
                $ImageAreaCaches.put(v_CacheKey ,v_CacheDatas ,$CacheTimeLen);
            }
            
            if ( Help.isNull(v_CacheDatas) ) return;
            
            for (ImageAreaInfo v_ImageArea : v_CacheDatas)
            {
                HSSFPatriarch    v_ToPatriarch = ((HSSFSheet)i_ToSheet).createDrawingPatriarch();
                HSSFClientAnchor v_ToAnchor    = new HSSFClientAnchor(Math.min(v_ImageArea.getAnchor().getDx1() ,1023)
                                                                     ,Math.min(v_ImageArea.getAnchor().getDy1() ,255)
                                                                     ,Math.min(v_ImageArea.getAnchor().getDx2() ,1023)
                                                                     ,Math.min(v_ImageArea.getAnchor().getDy2() ,255)
                                                                     ,v_ImageArea.getAnchor().getCol1()
                                                                     ,v_ImageArea.getAnchor().getRow1() + i_OffsetRow
                                                                     ,v_ImageArea.getAnchor().getCol2()
                                                                     ,v_ImageArea.getAnchor().getRow2() + i_OffsetRow);
                
                v_ToAnchor.setAnchorType(v_ImageArea.getAnchor().getAnchorType());
                
                v_ToPatriarch.createPicture(v_ToAnchor
                                           ,i_ToSheet.getWorkbook().addPicture(v_ImageArea.getPictureData().getData() ,v_ImageArea.getPictureData().getPictureType()));
            }
        }
        else if ( i_ToSheet instanceof SXSSFSheet )
        {
            if ( v_CacheDatas == null )
            {
                v_CacheDatas = new ArrayList<ImageAreaInfo>();
                
                XSSFSheet v_FromSheet = (XSSFSheet) i_FromSheet;
                
                if ( i_FromSheet.getDrawingPatriarch() != null ) 
                {
                    for (XSSFShape v_Shape : v_FromSheet.getDrawingPatriarch().getShapes()) 
                    {
                        if ( v_Shape instanceof XSSFPicture) 
                        {
                            XSSFPicture      v_Picture       = (XSSFPicture) v_Shape;
                            XSSFClientAnchor v_Anchor        = v_Picture.getClientAnchor();
                            XSSFPictureData  v_PictureData   = v_Picture.getPictureData();
                            
                            if ( i_AreaBeginRow <= v_Anchor.getRow1() 
                              && i_AreaEndRow   >= v_Anchor.getRow2() )
                            {
                                // Nothing. 在数据区域内的图片
                            }
                            else
                            {
                                continue;
                            }
                            
                            v_CacheDatas.add(new ImageAreaInfo(v_Anchor ,v_PictureData));
                        }
                    }
                }
                
                $ImageAreaCaches.put(v_CacheKey ,v_CacheDatas ,$CacheTimeLen);
            }
            
            if ( Help.isNull(v_CacheDatas) ) return;
            
            for (ImageAreaInfo v_ImageArea : v_CacheDatas)
            {
                SXSSFDrawing     v_ToPatriarch = ((SXSSFSheet)i_ToSheet).createDrawingPatriarch();
                XSSFClientAnchor v_ToAnchor    = new XSSFClientAnchor(v_ImageArea.getAnchor().getDx1()
                                                                     ,v_ImageArea.getAnchor().getDy1()
                                                                     ,v_ImageArea.getAnchor().getDx2()
                                                                     ,v_ImageArea.getAnchor().getDy2()
                                                                     ,v_ImageArea.getAnchor().getCol1()
                                                                     ,v_ImageArea.getAnchor().getRow1() + i_OffsetRow
                                                                     ,v_ImageArea.getAnchor().getCol2()
                                                                     ,v_ImageArea.getAnchor().getRow2() + i_OffsetRow);
                
                v_ToAnchor.setAnchorType(v_ImageArea.getAnchor().getAnchorType());
                
                copyClientAnchor((XSSFClientAnchor)v_ImageArea.getAnchor() ,v_ToAnchor ,i_OffsetRow);
                
                v_ToPatriarch.createPicture(v_ToAnchor
                                           ,i_ToSheet.getWorkbook().addPicture(v_ImageArea.getPictureData().getData() ,v_ImageArea.getPictureData().getPictureType()));
            }
        }
        else if ( i_ToSheet instanceof XSSFSheet )
        {
            if ( v_CacheDatas == null )
            {
                v_CacheDatas = new ArrayList<ImageAreaInfo>();
                
                XSSFSheet v_FromSheet = (XSSFSheet) i_FromSheet;
                
                if ( i_FromSheet.getDrawingPatriarch() != null ) 
                {
                    for (XSSFShape v_Shape : v_FromSheet.getDrawingPatriarch().getShapes()) 
                    {
                        if ( v_Shape instanceof XSSFPicture) 
                        {
                            XSSFPicture      v_Picture       = (XSSFPicture) v_Shape;
                            XSSFClientAnchor v_Anchor        = v_Picture.getClientAnchor();
                            XSSFPictureData  v_PictureData   = v_Picture.getPictureData();
                            
                            if ( i_AreaBeginRow <= v_Anchor.getRow1() 
                              && i_AreaEndRow   >= v_Anchor.getRow2() )
                            {
                                // Nothing. 在数据区域内的图片
                            }
                            else
                            {
                                continue;
                            }
                            
                            v_CacheDatas.add(new ImageAreaInfo(v_Anchor ,v_PictureData));
                        }
                    }
                }
                
                $ImageAreaCaches.put(v_CacheKey ,v_CacheDatas ,$CacheTimeLen);
            }
            
            if ( Help.isNull(v_CacheDatas) ) return;
            
            for (ImageAreaInfo v_ImageArea : v_CacheDatas)
            {
                XSSFDrawing      v_ToPatriarch = ((XSSFSheet)i_ToSheet).createDrawingPatriarch();
                XSSFClientAnchor v_ToAnchor    = new XSSFClientAnchor(v_ImageArea.getAnchor().getDx1()
                                                                     ,v_ImageArea.getAnchor().getDy1()
                                                                     ,v_ImageArea.getAnchor().getDx2()
                                                                     ,v_ImageArea.getAnchor().getDy2()
                                                                     ,v_ImageArea.getAnchor().getCol1()
                                                                     ,v_ImageArea.getAnchor().getRow1() + i_OffsetRow
                                                                     ,v_ImageArea.getAnchor().getCol2()
                                                                     ,v_ImageArea.getAnchor().getRow2() + i_OffsetRow);
                
                v_ToAnchor.setAnchorType(v_ImageArea.getAnchor().getAnchorType());
                
                copyClientAnchor((XSSFClientAnchor)v_ImageArea.getAnchor() ,v_ToAnchor ,i_OffsetRow);
                
                v_ToPatriarch.createPicture(v_ToAnchor
                                           ,i_ToSheet.getWorkbook().addPicture(v_ImageArea.getPictureData().getData() ,v_ImageArea.getPictureData().getPictureType()));
            }
        }
    }
    
    
    
    /**
     * 复制锚。用于2007的Excel文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-19
     * @version     v1.0
     *
     * @param i_FromAnchor
     * @param i_ToAnchor
     * @param i_OffsetRow     偏移行号
     */
    public final static void copyClientAnchor(XSSFClientAnchor i_FromAnchor ,XSSFClientAnchor i_ToAnchor ,int i_OffsetRow)
    {
        copyCTMarker(i_FromAnchor.getFrom() ,i_ToAnchor.getFrom() ,i_OffsetRow);
        copyCTMarker(i_FromAnchor.getTo()   ,i_ToAnchor.getTo()   ,i_OffsetRow);
    }
    
    
    
    /**
     * 复制标记。用于2007的Excel文件
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-19
     * @version     v1.0
     *
     * @param i_FromCTMarker
     * @param i_ToCTMarker
     * @param i_OffsetRow     偏移行号
     */
    public final static void copyCTMarker(CTMarker i_FromCTMarker ,CTMarker i_ToCTMarker ,int i_OffsetRow)
    {
        i_ToCTMarker.setRow(   i_FromCTMarker.getRow() + i_OffsetRow);
        i_ToCTMarker.setRowOff(i_FromCTMarker.getRowOff());
        i_ToCTMarker.setCol(   i_FromCTMarker.getCol());
        i_ToCTMarker.setColOff(i_FromCTMarker.getColOff());
    }
    
    
    
    /**
     * 复制字体
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-18
     * @version     v1.0
     *
     * @param i_FromFont  源字体
     * @param i_ToFont    目标字体
     */
    public final static void copyFont(Font i_FromFont ,Font i_ToFont)
    {
        i_ToFont.setBold(              i_FromFont.getBold());
        i_ToFont.setCharSet(           i_FromFont.getCharSet());
        i_ToFont.setColor(             i_FromFont.getColor());
        i_ToFont.setFontHeight(        i_FromFont.getFontHeight());
        i_ToFont.setFontHeightInPoints(i_FromFont.getFontHeightInPoints());
        i_ToFont.setFontName(          i_FromFont.getFontName());
        i_ToFont.setItalic(            i_FromFont.getItalic());
        i_ToFont.setStrikeout(         i_FromFont.getStrikeout());
        i_ToFont.setTypeOffset(        i_FromFont.getTypeOffset());
        i_ToFont.setUnderline(         i_FromFont.getUnderline());
    }
    
    
    
    /**
     * 复制单元格样式
     * 
     * @author      ZhengWei(HY)
     * @createDate  2017-03-18
     * @version     v1.0
     *
     * @param i_FromCellStyle  源单元格样式
     * @param i_ToCellStyle    目标单元格样式
     */
    public final static void copyCellStyle(CellStyle i_FromCellStyle ,CellStyle i_ToCellStyle)
    {
        if ( i_FromCellStyle instanceof HSSFCellStyle )
        {
            i_ToCellStyle.cloneStyleFrom(i_FromCellStyle);
            /*
            i_ToCellStyle.setAlignment(          i_FromCellStyle.getAlignmentEnum());
            i_ToCellStyle.setDataFormat(         i_FromCellStyle.getDataFormat());
            
            // 边框和边框颜色
            i_ToCellStyle.setBorderBottom(       i_FromCellStyle.getBorderBottomEnum());
            i_ToCellStyle.setBorderLeft(         i_FromCellStyle.getBorderLeftEnum());
            i_ToCellStyle.setBorderRight(        i_FromCellStyle.getBorderRightEnum());
            i_ToCellStyle.setBorderTop(          i_FromCellStyle.getBorderTopEnum());
            i_ToCellStyle.setLeftBorderColor(    i_FromCellStyle.getLeftBorderColor());
            i_ToCellStyle.setRightBorderColor(   i_FromCellStyle.getRightBorderColor());
            i_ToCellStyle.setTopBorderColor(     i_FromCellStyle.getTopBorderColor());
            i_ToCellStyle.setBottomBorderColor(  i_FromCellStyle.getBottomBorderColor());
            
            // 背景和前景
            i_ToCellStyle.setFillBackgroundColor(i_FromCellStyle.getFillBackgroundColor());
            i_ToCellStyle.setFillForegroundColor(i_FromCellStyle.getFillForegroundColor());
            i_ToCellStyle.setFillPattern(        i_FromCellStyle.getFillPatternEnum());
            i_ToCellStyle.setHidden(             i_FromCellStyle.getHidden());
            
            // 首行缩进
            i_ToCellStyle.setIndention(          i_FromCellStyle.getIndention());
            i_ToCellStyle.setLocked(             i_FromCellStyle.getLocked());
  
            // 旋转
            i_ToCellStyle.setShrinkToFit(        i_FromCellStyle.getShrinkToFit());
            i_ToCellStyle.setRotation(           i_FromCellStyle.getRotation());
            i_ToCellStyle.setVerticalAlignment(  i_FromCellStyle.getVerticalAlignmentEnum());
            i_ToCellStyle.setWrapText(           i_FromCellStyle.getWrapText());
            */
        }
        else if ( i_FromCellStyle instanceof XSSFCellStyle )
        {
            i_ToCellStyle.cloneStyleFrom(i_FromCellStyle);
        }
    }
    
}