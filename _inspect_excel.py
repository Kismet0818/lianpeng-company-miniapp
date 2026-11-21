import openpyxl
wb = openpyxl.load_workbook(r'C:\Users\Admin\Desktop\上海各区小学梯队.xlsx')
ws = wb['高中']
for r in range(1, ws.max_row+1):
    vals = []
    for c in range(1, 6):
        v = ws.cell(r, c).value
        vals.append(v)
    print(r, '|'.join('' if v is None else str(v).replace('\n','\\n') for v in vals))