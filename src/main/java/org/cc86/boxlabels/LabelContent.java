package org.cc86.boxlabels;

import java.util.List;

public class LabelContent
{
    private String column;
    private int row;
    
    public String getSection()
    {
        return section;
    }
    
    public void setSection(String section)
    {
        this.section = section;
    }
    
    private String section;
    private List<String> lines;
    
    private int BoxId;
    
    public String getColumn()
    {
        return column;
    }
    
    public void setColumn(String column)
    {
        this.column = column;
    }
    
    public int getRow()
    {
        return row;
    }
    
    public void setRow(int row)
    {
        this.row = row;
    }
    
    public List<String> getLines()
    {
        return lines;
    }
    
    public void setLines(List<String> lines)
    {
        this.lines = lines;
    }
    
    public int getBoxId()
    {
        return BoxId;
    }
    
    public void setBoxId(int boxId)
    {
        BoxId = boxId;
    }
    
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        LabelContent that = (LabelContent) o;
        
        if (row != that.row) return false;
        if (BoxId != that.BoxId) return false;
        if (!column.equals(that.column)) return false;
        if (!section.equals(that.section)) return false;
        return lines.equals(that.lines);
    }
    
    @Override
    public int hashCode()
    {
        int result = column.hashCode();
        result = 31 * result + row;
        result = 31 * result + lines.hashCode();
        result = 31 * result + BoxId;
        result = 31 * result + section.hashCode();
        return result;
    }
}
