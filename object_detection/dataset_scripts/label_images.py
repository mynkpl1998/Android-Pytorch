import os
from glob import glob
import PySimpleGUI as sg
from PIL import Image
import numpy as np
import base64

display_resolution = (800, 800)

sg.theme('DarkAmber')   # Add a touch of color
sg.set_options(font=('Arial Bold', 16))

def jpg2png(img: str):
    image = Image.open(img)
    # Get the image dimensions
    width, height = image.size

    # Calculate the center coordinates
    center_x = width // 2
    center_y = height // 2

    # Determine the crop size
    crop_size = min(width, height)

    # Calculate the top-left corner coordinates of the cropped image
    left = center_x - crop_size // 2
    top = center_y - crop_size // 2

    # Crop the image
    cropped_image = image.crop((left, top, left + crop_size, top + crop_size))
    cropped_image = cropped_image.resize(display_resolution, Image.ANTIALIAS)

    cropped_image.save("/tmp/tmp.png")
    return "/tmp/tmp.png"

def validateFolderPaths(
        src:str,
        dst:str):
    
    if len(src) == 0:
        return False, "Invalid path, len is zero"
    
    if len(dst) == 0:
        return False, "Invalid path, len is zero"
    
    if src == dst:
        return False, "Please specify different src and dest path"
    
    if not os.path.isdir(src):
        return False, "Path: %s doesn't exists"%(src) 

    if not os.path.isdir(dst):
        return False, "Path: %s doesn't exists"%(dst)
    return True, ""

def readImages(path: str,
               formats: list):
    wildcards = ""
    for format in formats:
        wildcards += "*.%s"%(format) + "|"
    wildcards = wildcards[:-1]
    search_str = "%s/"%(path) + wildcards
    print(search_str)
    return glob(search_str)

# All the stuff inside your window.
layout = [  
    
            [sg.Text('Images root path: '), sg.Button("Select Path", key='--select-root-img-path--')],
            [sg.Text('Output path: '), sg.Button("Select Path", key='--select-processed-img-path--')],
            [sg.Text('Selected Image Root Path: '), sg.Text("", key="--img-root-path--")],
            [sg.Text('Selected Output Path: '), sg.Text("", key="--proc-root-path--")],
            [sg.Text('File Formats: '), sg.Checkbox('jpg', key='--jpg--'), sg.Checkbox('png', key='--png--')],
            [sg.Text('Labels: '), sg.Combo(values=[], expand_x=True, key='--labels-list--')],
            [sg.Input('', key='--label-input--'), sg.Button('Add', key='--label-add--'), sg.Button('Reset', key='--label-clear--') ],

            [sg.Button('Lock', key = '--lock-button--')],

            [sg.HSeparator()],

            [ sg.Text("Num of Images: "), sg.Text('', key='--num-of-images--') ],
            [ sg.Text('Index: '), sg.Text('', key='--img-index--') ],
            [ sg.Image(key="--image--") ],
            [ sg.Button('Next', key="--next--"), sg.Button('Label', key="--label-img--")]
             ] 


labels_list = []
selected_file_formats = []
images_list = []
img_index = 0

# Create the Window
window = sg.Window(__file__, layout)
window_created = False

# Event Loop to process "events" and get the "values" of the inputs
while True:
    event, values = window.read()
    window['--labels-list--'].update(values=labels_list)
    
    if event == sg.WIN_CLOSED or event == 'Cancel': # if user closes window or clicks cancel
        break
    if event == '--select-root-img-path--':
        folder_path = sg.PopupGetFolder('Select Folder')
        window["--img-root-path--"].update(folder_path)

    if event == '--select-processed-img-path--':
        folder_path = sg.PopupGetFolder('Select Folder')
        window["--proc-root-path--"].update(folder_path)
    
    # Lock src and dst
    if event == '--lock-button--':
        
        if values['--jpg--']:
            selected_file_formats.append('jpg')
        if values['--png--']:
            selected_file_formats.append('png')

        src = window["--img-root-path--"]
        dst = window["--proc-root-path--"]
        
        # Validate folder paths
        res, msg = validateFolderPaths(src.get(), dst.get())
        if not res:
            sg.Popup(msg)
        
        if len(selected_file_formats) == 0:
            sg.Popup("Pls select file formats")
            res = False
        
        # Validate labels
        if len(labels_list) == 0:
            sg.Popup("No labels supplied.")
            res = False
        
        if res:

            window['--select-root-img-path--'].update(disabled=True)
            window['--select-processed-img-path--'].update(disabled=True)
            window['--lock-button--'].update(disabled=True)
            window['--jpg--'].update(disabled=True)
            window['--png--'].update(disabled=True)

            # Load images
            print(src.get())
            print(selected_file_formats)
            images_list = readImages(src.get(), selected_file_formats)
            print("Found ", len(images_list))
            window['--num-of-images--'].update("%d"%(len(images_list)))
            window['--image--'].update(filename=jpg2png(images_list[img_index]))
            window['--img-index--'].update("%d/%d"%(img_index+1, len(images_list)))

    if event == '--next--':
        img_index += 1
        if img_index >= len(images_list):
            img_index = 0
        window['--image--'].update(filename=jpg2png(images_list[img_index]))
        window['--img-index--'].update("%d/%d"%(img_index+1, len(images_list)))
    
    if event == '--label-add--':
        val = values['--label-input--']
        if val not in labels_list and len(val) > 0:
            labels_list.append(val)
        window['--labels-list--'].update(values=labels_list)
    
    if event == '--label-clear--':
        labels_list = []
        window['--labels-list--'].update(values=labels_list)
        

    print('You entered ', values)

window.close()